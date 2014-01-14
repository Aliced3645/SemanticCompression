package Metadata;

import java.sql.DriverManager;
import java.sql.SQLException;

import java.sql.Connection;

import moa.core.SerializeUtils;
import moa.streams.InstanceStream;
import WekaTraining.ColumnData;
import WekaTraining.Utilities;

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
		//It was in CSV format in Ashvin's code
		String compressedTableName = trainingTable + "_compressed";
        
		//Write models in binary format to database.
		for (int i = 1; i <= classified.length; i++) {
            ColumnData c = compressedColumns[i];
            //SerializeUtils.writeToFile(Utilities.getModelFile(_outputFolder, c._classIndex), c._classifier);
            
        }
	}
	
}
