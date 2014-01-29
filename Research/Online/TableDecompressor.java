package Online;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import weka.core.Instances;

/**
 * Decompress should be executed in the online phase.
 * 
 * @author shu
 */
public class TableDecompressor {

	// Has a jdbc driver connecting to database;
	private Connection connection;

	public TableDecompressor() throws SQLException {
		// by default : initialize connection
		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	InstancesHeader readHeader(String tableName) throws SQLException,
			IOException, ClassNotFoundException {
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("select header from headers where name =  '"
						+ tableName + "';");
		InstancesHeader header = null;
		if (resultSet.first()) {
			InputStream in = resultSet.getBinaryStream(1);
			// Store to a temp file...
			File file = new File("Temp");
			FileOutputStream fos = new FileOutputStream(file);
			byte[] data = new byte[4096];
			int count = -1;
			while ((count = in.read(data, 0, 4096)) != -1) {
				System.out.println(count);
				fos.write(data, 0, count);
			}
			fos.close();
			header = (InstancesHeader) SerializeUtils.readFromFile(file);
			System.out.println(header.numAttributes());
			System.out.println(header.toString());
		}
		return header;
	}

	int[] readClassified(String tableName) throws SQLException, IOException, ClassNotFoundException {
		int[] classfied = null;
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("select classified from classifieds where name =  '"
						+ tableName + "';");
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
			classfied = (int[]) SerializeUtils.readFromFile(file);
		}
		return classfied;
	}

	/**
	 * Decompress a column value using
	 * 
	 * @param tableName
	 * @param columnName
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	Instances decompressColumn(String tableName, String columnName)
			throws SQLException, IOException, ClassNotFoundException {
		// try to read the header
		InstancesHeader header = readHeader(tableName);
		int[] classfied = readClassified(tableName);
		
		return null;
	}
}
