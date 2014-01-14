package Metadata;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.sql.Connection;

import weka.core.Instances;

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
public class MySQLMetadataManager implements MetadataManager {

	// Has a jdbc driver connecting to database;
	private Connection connection;

	public MySQLMetadataManager() throws SQLException {
		// by default : initialize connection
		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void storeModels(String trainingTable, String originalDb,
			int[] classified, Instances trainingInstances,
			ColumnData[] compressedColumns, double errorThreshold)
			throws SQLException {
		
		trainingInstances.setClassIndex(0);
		InstanceStream trainingStream = new CachedInstancesStream(trainingInstances);
		
		// It was in CSV format in Ashvin's code
		String compressedTableName = trainingTable + "_compressed";
		// create original db connection
		Connection originalConnectoin = DriverManager
				.getConnection("jdbc:mysql://localhost/" + originalDb + "?"
						+ "user=shu&password=shu");
		
		/*
		 * Create table for storing model files if necessary.
		 * For each table to be compressed, there is a metadata table storing its models.
		 * There should be a table called "models" in metadata db.
		 * It has three columns:
		 *  - The table of the model to be applied to.
		 *  - The column name of this model to be compressed.
		 *  - The binary content of the model.
		 */
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery(
				"show tables like '" + trainingTable + "'");
		if(!resultSet.first()){
			//create this table
			statement.executeUpdate(
					"create table " + trainingTable + "(column CHAR(50), model LONGBLOB);");
		}
		
		for (int i = 0; i < classified.length; i++) {
			String sql = "Insert into metadata." + trainingTable + "(column, model) values (?, ?)";
			PreparedStatement ps = connection.prepareStatement(sql);
			ColumnData c = compressedColumns[i];
			String columnName = trainingInstances.attribute(i).name();
			ps.setString(1, columnName);
			
			//Get the binary model content..
			
			/*SerializeUtils.writeToFile(Utilities.getModelFile(_outputFolder,
					c._classIndex), c._classifier);
			*/
			
		}
	}

}
