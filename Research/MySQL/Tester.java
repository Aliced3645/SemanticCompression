package MySQL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import WekaTraining.Utilities;

/**
 * Used for testing MySQL JDBC integration.
 * 
 * @author shu
 * 
 */
public class Tester {

	private static Connection connection = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;

	public static void main(String[] args) {
		try {
			connection = DriverManager
					.getConnection("jdbc:mysql://localhost/metadata?"
							+ "user=shu&password=shu");
			statement = connection.createStatement();
			/*
			 * resultSet = statement.executeQuery(
			 * "select * from testdb.table1"); while(resultSet.next()){
			 * System.out.println(resultSet.getInt("id")); }
			 */
			/*
			 * statement.executeUpdate(" insert into table3 (a) values (1);");
			 */
			try {
				// Test loading binary content from database and recover to weka
				// objects.
				ResultSet resultSet = statement
						.executeQuery("select model from " + "table2" + " where attribute =  '"
								+ "A" + "';");
				if (resultSet.first()) {
					InputStream in = resultSet.getBinaryStream(1);
					// Store to a temp file...
					File file = new File("Temp");
					FileOutputStream fos = new FileOutputStream(file);
					byte[] data = new byte[4096];
					int count = -1;
					while ((count = in.read(data, 0, 4096)) != -1) {
						fos.write(data, 0, count);
					}
					fos.close();
					Classifier classifier = 
							(Classifier) SerializeUtils.readFromFile(file);
					if(classifier == null)
						System.out.println("huaca");
				}
			} catch (Exception e) {
				// delete table..
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}

				if (statement != null) {
					statement.close();
				}

				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {

			}
		}
	}
}
