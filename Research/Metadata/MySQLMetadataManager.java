package Metadata;

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
			throws SQLException, IOException {

		/*
		 * Create a table for storing compressed table. (originally in csv model).
		 * SO we store the csv content in binary blob. 
		 */
		trainingInstances.setClassIndex(0);
		InstanceStream trainingStream = new CachedInstancesStream(trainingInstances);
        InstancesHeader header = trainingStream.getHeader();
        StringBuilder sb = new StringBuilder();
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery(
				"show tables like 'compressed_tables'");
		if(!resultSet.first()){
			statement = connection.createStatement();
			statement.executeUpdate(
					"create table compresed_tables (name CHAR(50), csv LONGBLOB);");
		}
		String sql = "Insert into metadata.compressed_tables (name, csv) values (?, ?)";
		PreparedStatement ps = connection.prepareStatement(sql);
		FileOutputStream fos = new FileOutputStream(new File("temp"));
		while (trainingStream.hasMoreInstances()) {
            Instance inst = trainingStream.nextInstance();
            sb.setLength(0);
            //by columns
            for (int i = 1; i < compressedColumns.length; i++) {
                ColumnData c = compressedColumns[i];
                header.setClassIndex(i-1);
                inst.setDataset(header);
                if (c._classifier == null || !Utilities.compressable(c._classifier, inst, i, errorThreshold))
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
        ps.setString(1, trainingTable);
        ps.setBlob(2, in);
        ps.executeUpdate();
        ps.close();
        in.close();
		
		
		/*
		 * Create table for storing model files if necessary.
		 * For each table to be compressed, there is a metadata table storing its models.
		 * There should be a table called "models" in metadata db.
		 * It has three columns:
		 *  - The table of the model to be applied to.
		 *  - The column name of this model to be compressed.
		 *  - The binary content of the model.
		 */
		
		statement = connection.createStatement();
		resultSet = statement.executeQuery(
				"show tables like '" + trainingTable + "'");
		if(!resultSet.first()){
			//create this table
			statement.executeUpdate(
					"create table " + trainingTable + "(column CHAR(50), model LONGBLOB);");
		}
		
		for (int i = 0; i < classified.length; i++) {
			//Get the binary model content and write it to metadata table.
			sql = "Insert into metadata." + trainingTable + "(column, model) values (?, ?)";
			ps = connection.prepareStatement(sql);
			ColumnData c = compressedColumns[i];
			String columnName = trainingInstances.attribute(i).name();
			ps.setString(1, columnName);
			File modelFile = new File("model.moa." + columnName);
			SerializeUtils.writeToFile(modelFile,  c._classifier);
			in = new BufferedInputStream(new FileInputStream(modelFile)); 
			ps.setBinaryStream(2, in, (int) modelFile.length()); 
			ps.executeUpdate();
			/*SerializeUtils.writeToFile(Utilities.getModelFile(_outputFolder,
					c._classIndex), c._classifier);
			*/
			in.close();
		}
		connection.close();
		
	}

}
