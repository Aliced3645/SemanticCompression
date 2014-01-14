package Metadata;

import java.sql.DriverManager;
import java.sql.SQLException;

import java.sql.Connection;

import moa.streams.InstanceStream;
import WekaTraining.ColumnData;

/**
 * Metadata manager, a java class managing matadata storage / database information.
 * @author shu
 */
public class MySQLMetadataManager implements MetadataManager{

	//Has a jdbc driver connecting to database;
	private Connection connection;
	
	public MySQLMetadataManager() throws SQLException{
		//by default : initialize connection
		connection = DriverManager.getConnection("jdbc:mysql://localhost/metadata?"
	              + "user=shu&password=shu");
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void storeModels(String trainingTable, String originalDb,
			int[] classified, InstanceStream inputStream,
			ColumnData[] compressedColumns, double errorThreshold) {
		
	}
	
}
