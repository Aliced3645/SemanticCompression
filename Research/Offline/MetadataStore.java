package Offline;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import WekaTraining.CustomWEKAClassifier;
import WekaTraining.Utilities;
import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;

/**
 * New class used to store metadata.
 * 
 * @author shu
 * 
 */
public class MetadataStore {

	Connection connection = null;

	public MetadataStore(Connection connection) {
		this.connection = connection;
	}

	// Helper function to read header and classifiers
	InstancesHeader header;
	Classifier[] classifiers;

	void readHeaderAndClassifiers(File inputFolder) throws IOException,
			ClassNotFoundException {
		this.header = (InstancesHeader) SerializeUtils.readFromFile(Utilities
				.getHeaderFile(inputFolder));

		this.classifiers = new Classifier[header.numAttributes() + 1];
		// store all the classifiers and fileinputstreams
		for (int i = 1; i <= header.numAttributes(); i++) {
			try {
				classifiers[i] = (Classifier) SerializeUtils
						.readFromFile(Utilities.getModelFile(inputFolder, i));
				// System.out.println("=========================");
				// System.out.println(_header.attribute(i - 1).name());
				// System.out.println(_classifiers[i]);
			} catch (IOException e) {
				// WE here can allow classifier to be a null value.
				classifiers[i] = null;
			}
		}

	}

	/**
	 * Helper function to copy metadata into databases;
	 * 
	 * @param connection
	 * @param sql
	 * @param name
	 * @param in
	 * @param depends
	 * @throws SQLException
	 */
	static void makeInsertStatementAndExecute(Connection connection,
			String sql, String name, InputStream in, String depends)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setString(1, name);
		ps.setBlob(2, in);
		ps.setString(3, depends);
		ps.execute();
		ps.close();
	}

	/**
	 * Helper function to copy header into database;
	 * 
	 * @param connection
	 * @param sql
	 * @param name
	 * @param in
	 * @param depends
	 * @throws SQLException
	 */
	void makeInsertStatementAndExecuteForHeader(Connection connection,
			String sql, String name, InputStream in) throws SQLException {
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setString(1, name);
		ps.setBlob(2, in);
		ps.execute();
		ps.close();
	}

	// Store models and their dependencies into MySQL
	// the model table should be:
	// <column name, blob of the model, string of dependencies>
	public void store(String inputFolderString, String tableName) throws IOException,
			ClassNotFoundException, SQLException {

		File inputFolder = new File(inputFolderString);
		readHeaderAndClassifiers(inputFolder);

		HashMap<String, moa.classifiers.Classifier> nameMap = new HashMap<String, moa.classifiers.Classifier>();
		HashMap<String, String> dependencyMap = new HashMap<String, String>();
		// Get the names
		for (int i = 1; i <= header.numAttributes(); i++) {
			String columnName = header.attribute(i - 1).name();
			nameMap.put(columnName, classifiers[i]);
		}
		StringBuilder sb = new StringBuilder();
		HashSet<String> found = new HashSet<String>();

		// Get the dependencies;
		Pattern treeDescriptionLine = Pattern.compile("^[\\s|]*\\b(\\w+)\\b");
		for (int i = 1; i < classifiers.length; i++) {
			if (classifiers[i] != null) {
				found.clear();
				sb.setLength(0);
				String this_col = header.attribute(i - 1).name();
				if (!(classifiers[i] instanceof CustomWEKAClassifier))
					continue;
				// depended columns
				LinkedList<String> depends = new LinkedList<String>();
				String description = ((CustomWEKAClassifier) classifiers[i])
						.getWEKAClassifier().toString();
				String[] lines = description.split("\n", -1);
				for (String line : lines) {
					if (!line.contains(":")
							|| !(line.contains("<") || line.contains(">") || line
									.contains("=")))
						continue;
					Matcher m = treeDescriptionLine.matcher(line);
					if (!m.find())
						continue;
					String other_col = m.group(1);
					if (found.contains(other_col)
							|| !nameMap.containsKey(other_col))
						continue;
					depends.add(other_col);
					found.add(other_col);
				}
				// make the string, seperated by colons;
				for (String depend : depends) {
					sb.append(depend);
					sb.append(",");
				}
				if (sb.length() != 0) {
					sb.deleteCharAt(sb.length() - 1);
					dependencyMap.put(this_col, sb.toString());
				}
			}
		}

		// put them into database;
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("show tables like '"
				+ tableName + "'");

		if (resultSet.first()) {
			statement = connection.createStatement();
			statement.executeUpdate("drop table " + tableName);
		}

		String sentence = "create table " + tableName
				+ " (attribute CHAR(50), model LONGBLOB, dependencies TEXT);";
		statement.executeUpdate(sentence);
		String sql = "Insert into metadata." + tableName
				+ " (attribute, model, dependencies) values (?, ?, ?)";

		// insert the dependencies and classifiers.
		for (String column : nameMap.keySet()) {
			File modelFile = new File("temp");
			SerializeUtils.writeToFile(modelFile, nameMap.get(column));
			InputStream in = new BufferedInputStream(new FileInputStream(
					modelFile));
			String depends = dependencyMap.get(column);
			if (depends == null)
				depends = "null";
			makeInsertStatementAndExecute(connection, sql, column, in, depends);
			in.close();
		}

		// store the header inside metadata database;
		InstancesHeader header = this.header;
		statement = connection.createStatement();
		resultSet = statement.executeQuery("show tables like 'headers'");
		if (!resultSet.first()) {
			statement = connection.createStatement();
			statement
					.executeUpdate("create table headers (name CHAR(50), header LONGBLOB);");
			statement = connection.createStatement();
			statement
					.executeUpdate("alter table headers add constraint pk_t unique key (name);");
		}
		sql = "Insert into headers (name, header) values (?, ?)";
		File headerFile = new File("temp");
		SerializeUtils.writeToFile(headerFile, header);
		InputStream in = new BufferedInputStream(
				new FileInputStream(headerFile));
		try {
			makeInsertStatementAndExecuteForHeader(connection, sql, tableName,
					in);
		} catch (MySQLIntegrityConstraintViolationException e) {
			statement = connection.createStatement();
			statement.executeUpdate("delete from headers where name = '"
					+ tableName + "';");
			in = new BufferedInputStream(new FileInputStream(headerFile));
			makeInsertStatementAndExecuteForHeader(connection, sql, tableName,
					in);
		}
		in.close();

	}

	/**
	 * Testing
	 * 
	 * @param args
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, IOException,
			ClassNotFoundException {
		String inputFolder = args[0];
		String tableName = args[1];
		Connection connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
		MetadataStore store = new MetadataStore(connection);
		store.store(inputFolder, tableName);

	}
}
