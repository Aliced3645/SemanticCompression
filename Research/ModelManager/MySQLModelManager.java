package ModelManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import weka.core.Instance;
import weka.core.Instances;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import moa.streams.CachedInstancesStream;
import moa.streams.InstanceStream;
import WekaTraining.ColumnData;
import WekaTraining.Utilities;

/**
 * Metadata manager, a java class managing matadata storage / database
 * information.
 * 
 * @author shu
 */
public class MySQLModelManager implements ModelManager {

	// Has a jdbc driver connecting to database;
	private Connection connection;

	public MySQLModelManager() throws SQLException {
		// by default : initialize connection
		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Helper function for code reuse.
	 * @param sql
	 * @param name
	 * @param blob
	 * @return
	 * @throws SQLException 
	 */
	void makeInsertStatementAndExecute(
			String sql, String name, InputStream in) throws SQLException{
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setString(1, name);
		ps.setBlob(2, in);
		ps.execute();
		ps.close();
	}
	
	@Override
	public void storeModels(String trainingTable, String originalDb,
			int[] classified, Instances trainingInstances,
			ColumnData[] compressedColumns, double errorThreshold)
			throws SQLException, IOException {

		InstanceStream trainingStream = new CachedInstancesStream(
				trainingInstances);
		InstancesHeader header = trainingStream.getHeader();
		/*
		 * Create a table for storing compressed table. (originally in csv
		 * model). SO we store the csv content in binary blob.
		 */
		trainingInstances.setClassIndex(0);
		StringBuilder sb = new StringBuilder();
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("show tables like 'compressed_tables'");
		if (!resultSet.first()) {
			statement = connection.createStatement();
			statement
					.executeUpdate("create table compressed_tables (name CHAR(50), csv LONGBLOB);");
			statement = connection.createStatement();
			statement
					.executeUpdate("alter table compressed_tables add constraint pk_t unique key (name);");
		}
		String sql = "Insert into compressed_tables (name, csv) values (?, ?)";
		FileOutputStream fos = new FileOutputStream(new File("temp"));
		while (trainingStream.hasMoreInstances()) {
			Instance inst = trainingStream.nextInstance();
			sb.setLength(0);
			// by columns
			for (int i = 1; i < compressedColumns.length; i++) {
				ColumnData c = compressedColumns[i];
				header.setClassIndex(i - 1);
				inst.setDataset(header);
				if (c._classifier == null
						|| !Utilities.compressable(c._classifier, inst, i,
								errorThreshold))
					sb.append(Utilities.stringValue(inst, c._classIndex));
				sb.append(",");
			}
			// replace the last comma with a newline
			sb.replace(sb.length() - 1, sb.length(), "\n");
			fos.write(sb.toString().getBytes());
		}
		fos.close();
		File csvFile = new File("temp");
		InputStream in = new BufferedInputStream(new FileInputStream(csvFile));
		
		try {
			makeInsertStatementAndExecute(sql, trainingTable, in);
		} catch (MySQLIntegrityConstraintViolationException e) {
			statement = connection.createStatement();
			statement
					.executeUpdate("delete from compressed_tables where name = '"
							+ trainingTable + "';");
			in = new BufferedInputStream(new FileInputStream(csvFile));
			makeInsertStatementAndExecute(sql, trainingTable, in);
		}
		in.close();

		/*
		 * Create a table for storing header content...
		 */
		statement = connection.createStatement();
		resultSet = statement
				.executeQuery("show tables like 'headers'");
		if(!resultSet.first()){
			statement = connection.createStatement();
			statement.executeUpdate("create table headers (name CHAR(50), header LONGBLOB);");
			statement = connection.createStatement();
			statement
			.executeUpdate("alter table headers add constraint pk_t unique key (name);");
		}
		sql = "Insert into headers (name, header) values (?, ?)";
		File headerFile = new File("temp");
		SerializeUtils.writeToFile(headerFile, trainingStream.getHeader());
		in = new BufferedInputStream(new FileInputStream(headerFile));
		try {
			makeInsertStatementAndExecute(sql, trainingTable, in);
		} catch (MySQLIntegrityConstraintViolationException e){
			statement = connection.createStatement();
			statement
			.executeUpdate("delete from headers where name = '"
					+ trainingTable + "';");
			in = new BufferedInputStream(new FileInputStream(headerFile));
			makeInsertStatementAndExecute(sql, trainingTable, in);
		}
		in.close();
		
		/*
		 * Create a table for storing classified[]..
		 */
		statement = connection.createStatement();
		resultSet = statement
				.executeQuery("show tables like 'classifieds'");
		if(!resultSet.first()){
			statement = connection.createStatement();
			statement.executeUpdate("create table classifieds (name CHAR(50), classified LONGBLOB);");
			statement = connection.createStatement();
			statement
			.executeUpdate("alter table classifieds add constraint pk_t unique key (name);");
		}
		sql = "Insert into classifieds (name, classified) values (?, ?)";
		File classifiedFile = new File("temp");
		SerializeUtils.writeToFile(classifiedFile, classified);
		in = new BufferedInputStream(new FileInputStream(classifiedFile));
		try {
			makeInsertStatementAndExecute(sql, trainingTable, in);
		} catch (MySQLIntegrityConstraintViolationException e){
			statement = connection.createStatement();
			statement
			.executeUpdate("delete from classifieds where name = '"
					+ trainingTable + "';");
			in = new BufferedInputStream(new FileInputStream(classifiedFile));
			makeInsertStatementAndExecute(sql, trainingTable, in);
		}
		in.close();
		
		/*
		 * Create table for storing model files if necessary. For each table to
		 * be compressed, there is a metadata table storing its models. There
		 * should be a table called "models" in metadata db. It has three
		 * columns: - The table of the model to be applied to. - The column name
		 * of this model to be compressed. - The binary content of the model.
		 */

		statement = connection.createStatement();
		resultSet = statement.executeQuery("show tables like '" + trainingTable
				+ "'");

		if (resultSet.first()) {
			statement = connection.createStatement();
			statement.executeUpdate("drop table " + trainingTable);
		}
		// create this table
		String sentence = "create table " + trainingTable
				+ " (attribute CHAR(50), model LONGBLOB);";
		statement.executeUpdate(sentence);

		for (int i = 1; i <= classified.length; i++) {
			// Get the binary model content and write it to metadata table.
			sql = "Insert into metadata." + trainingTable
					+ " (attribute, model) values (?, ?)";
			ColumnData c = compressedColumns[i];
			String columnName = trainingInstances.attribute(i - 1).name();
			//System.out.println(columnName);
			File modelFile = new File("model.moa." + columnName);
			SerializeUtils.writeToFile(modelFile, c._classifier);
			in = new BufferedInputStream(new FileInputStream(modelFile));
			makeInsertStatementAndExecute(sql, columnName, in);
			in.close();

			modelFile = modelFile.getCanonicalFile();
			modelFile.delete();
				

		}

		connection.close();

	}

}
