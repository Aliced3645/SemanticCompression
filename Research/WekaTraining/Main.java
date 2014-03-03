package WekaTraining;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import moa.core.InstancesHeader;
import moa.core.SerializeUtils;

import ModelManager.ModelManager;
import ModelManager.MySQLModelManager;

/**
 * Package: semanticcompression Class: Main Description: CLI interface for the
 * program.
 */
public class Main {

	// compress sqlPhotoObjAll-2-5061-137_0015.arff - classified -1
	// decompress classified output.arff
	// compress covtype.arff - classified .05 sampledTrials.txt
	// decompress classified output.arff

	private static final String DEFAULT_SAMPLED_TRIALS_FILE = "sampledTrials.txt";

	private static void printUsageAndExit() {
		System.out
				.println("usage: compress <training .arff file> <testing .arff file> <output folder> <error threshold> <optional: sampled trials file>");
		System.out
				.println("note: a negative error threshold tells the program to use the default error threshold");
		System.out
				.println("usage: decompress <input folder> <output .arff file> <output .dot graph file>");
		System.out
				.println("note: if testing file is '-', then training file is used for testing as well");
		System.out
				.println("note: if sampled trials file is not provided, it will be generated and output to '"
						+ DEFAULT_SAMPLED_TRIALS_FILE + "'");
		System.exit(1);
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
	 * @param connection
	 * @param sql
	 * @param name
	 * @param in
	 * @param depends
	 * @throws SQLException
	 */
	static void makeInsertStatementAndExecuteForHeader(Connection connection,
			String sql, String name, InputStream in)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setString(1, name);
		ps.setBlob(2, in);
		ps.execute();
		ps.close();
	}
	
	/**
	 * 
	 * @param args
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws IOException, SQLException {
		/*
		 * if (args.length < 1 || !(args[0].equals("compress") ||
		 * args[0].equals("decompress"))) { printUsageAndExit(); }
		 */
		if (args[0].equals("compress")) {
			if (args.length != 5 && args.length != 6)
				printUsageAndExit();

			String trainingFile = args[1];
			String testingFile = args[2];
			String outputFolder = args[3];
			double errorThreshold = Double.parseDouble(args[4]);
			if (testingFile.equals("-"))
				testingFile = trainingFile;
			PriorityQueue<ColumnData> columnData;

			if (args.length == 6) {
				System.out.println("Reading sampled trials file:");
				columnData = Utilities.readColumnData(args[5]);
				if (columnData == null) {
					System.out
							.println("Error when reading sampled trials file");
					System.exit(1);
				}
			} else {
				System.out.println("Creating sampled trials file:");
				columnData = IterativeCompression
						.calculateColumnData(trainingFile);
				Utilities.writeColumnData(DEFAULT_SAMPLED_TRIALS_FILE,
						new PriorityQueue<ColumnData>(columnData));
			}

			System.out.println("Classifying and compressing the data:");
			IterativeCompression ic = new IterativeCompression(trainingFile,
					testingFile, columnData, outputFolder, errorThreshold);
			ic.run();

		} else if (args[0].equals("decompress")) {
			if (args.length != 4)
				printUsageAndExit();

			String inputFolder = args[1];
			String outputFile = args[2];
			String graphFile = args[3];
			Decompression dc = new Decompression(inputFolder, outputFile,
					graphFile);
			dc.run();

		} else if (args[0].equals("store")) {

			String inputFolder = args[1];
			String tableName = args[2];
			// adding the model manager stuff here...
			// so now we have models, just push them into ModelManager inside
			// MySQL.
			Connection connection = DriverManager
					.getConnection("jdbc:mysql://localhost/metadata?"
							+ "user=shu&password=shu");
			// the model table should be: <column name, blob of the model,
			// string of dependencies>
			DecompressionStream inStream = new DecompressionStream(inputFolder);
			moa.classifiers.Classifier[] classifiers = inStream
					.getClassifiers();
			// The hashmap of name and classifier
			HashMap<String, moa.classifiers.Classifier> nameMap = new HashMap<String, moa.classifiers.Classifier>();
			// The hashmap of column name and its dependency columns
			HashMap<String, String> dependencyMap = new HashMap<String, String>();
			// Get the names
			for (int i = 1; i <= inStream.getHeader().numAttributes(); i++) {
				String columnName = inStream.getHeader().attribute(i - 1)
						.name();
				nameMap.put(columnName, classifiers[i]);
			}
			StringBuilder sb = new StringBuilder();
			HashSet<String> found = new HashSet<String>();

			// Get the dependencies;
			Pattern treeDescriptionLine = Pattern
					.compile("^[\\s|]*\\b(\\w+)\\b");
			for (int i = 1; i < classifiers.length; i++) {
				if (classifiers[i] != null) {
					found.clear();
					sb.setLength(0);
					String this_col = inStream.getHeader().attribute(i - 1)
							.name();
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

			String sentence = "create table "
					+ tableName
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
				makeInsertStatementAndExecute(connection, sql, column, in,
						depends);
				in.close();
			}

			// store the header inside metadata database;
			InstancesHeader header = inStream.getHeader();
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
			InputStream in = new BufferedInputStream(new FileInputStream(
					headerFile));
			try {
				makeInsertStatementAndExecuteForHeader(connection, sql, tableName, in);
			} catch (MySQLIntegrityConstraintViolationException e) {
				statement = connection.createStatement();
				statement
				.executeUpdate("delete from headers where name = '"
						+ tableName + "';");
				in = new BufferedInputStream(new FileInputStream(headerFile));
				makeInsertStatementAndExecuteForHeader(connection, sql, tableName, in);
			}
			in.close();
			
		}
	}
}
