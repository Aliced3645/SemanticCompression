package Online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Decompress should be executed in the online phase.
 * 
 * @author shu
 */
public class TableDecompressor {

	// Has a jdbc driver connecting to database;
	private Connection connection;

	InstancesHeader header;
	int[] classfied;
	BufferedReader inputStream;

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

	int[] readClassified(String tableName) throws SQLException, IOException,
			ClassNotFoundException {
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

	BufferedReader readCompressedTable(String tableName) throws SQLException,
			IOException {
		BufferedReader inputStream = null;
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("select csv from compressed_tables where name =  '"
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
			inputStream = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));
		}
		return inputStream;
	}

	Classifier readClassifier(String tableName, String columnName)
			throws SQLException, IOException, ClassNotFoundException {
		Classifier classifier = null;
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("select model from '"
				+ tableName + "' where attribute =  '" + columnName + "';");
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
			classifier = (Classifier) SerializeUtils.readFromFile(file);
		}
		return classifier;
	}

	/**
	 * Return values directly. Now we just assume the array is not that "large".
	 * 
	 * @param classifier
	 * @return
	 */
	private Double[] iterativeDecompressColumn(Classifier classifier, String columnName) {
		List<Double> answers = new ArrayList<Double>();
		Instance instance = new DenseInstance(header.numAttributes());
		Attribute attribute = header.attribute(columnName);
		int index = attribute.index(); //not sure where index starts.
		while (true) {
			try {
				String line = inputStream.readLine();
				if (line == null || line.equals("")) {
					break;
				} else {
					String[] cols = line.split(",", -1);
					if (cols.length != header.numAttributes()) {
						break;
					} else {
						for (int i = 0; i < cols.length; i++) {
							if (cols[i].equals("")) {
								instance.setMissing(i);
							} else if (header.attribute(i).isNumeric()) {
								
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return (Double[]) (answers.toArray());
	}

	private Double[] getValuesDirectly(String columnName){
		List<Double> answers = new ArrayList<Double>();
		Attribute attribute = header.attribute(columnName);
		int index = attribute.index(); //not sure where index starts.
		while(true){
			try{
				String line = inputStream.readLine();
				if(line == null || line.equals("")){
					break;
				} else {
					String[] cols = line.split(",", -1);
					String value = cols[index];
					if(attribute.isNumeric()){
						try{
							Double val = Double.parseDouble(value);
							answers.add(val);
						} catch (NumberFormatException e) {
							e.printStackTrace();
							break;
						}
					}
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}
		return (Double[])(answers.toArray());
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
	Double[] decompressColumn(String tableName, String columnName)
			throws SQLException, IOException, ClassNotFoundException {
		// try to read the header
		header = readHeader(tableName);
		classfied = readClassified(tableName);
		inputStream = readCompressedTable(tableName);
		// Get the classifier of that column.
		// !!! It is possible that classifier is a null object..
		Classifier classifier = readClassifier(tableName, columnName);
		if(classifier == null){
			//read the value directly.
			return getValuesDirectly(columnName);
		}
		return iterativeDecompressColumn(classifier, columnName);
	}
}
