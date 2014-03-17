package Online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import WekaTraining.CustomWEKAClassifier;
import WekaTraining.Utilities;
import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import weka.classifiers.trees.M5P;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class DecompressByDependency {
	
	private Connection connection;
	
	InstancesHeader header;
	
	BufferedReader inputStream;
	
	static boolean MEASURE_TIME = true;
	
	public DecompressByDependency() throws SQLException {

		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public void readHeader(String tableName) throws SQLException, IOException,
			ClassNotFoundException {
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("select header from headers where name =  '"
						+ tableName + "';");

		if (resultSet.first()) {
			InputStream in = resultSet.getBinaryStream(1);
			File file = new File("Temp");
			FileOutputStream fos = new FileOutputStream(file);
			byte[] data = new byte[4096];
			int count = -1;
			while ((count = in.read(data, 0, 4096)) != -1) {
				fos.write(data, 0, count);
			}
			fos.close();
			header = (InstancesHeader) SerializeUtils.readFromFile(file);

		}

	}

	
	/**
	 * Helper function to get relevant columns.
	 * 
	 * @param tableName
	 * @param columnName
	 * @return
	 * @throws SQLException
	 */
	public List<String> getDependencies(String tableName, String columnName)
			throws SQLException {
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("select dependencies from " + tableName
						+ " where attribute =  '" + columnName + "';");

		String dependencies;
		if (resultSet.next()) {
			dependencies = resultSet.getString(1);
		} else {
			return null;
		}

		String[] dependArray = dependencies.split(",");

		if (dependArray[0].equals("null")) {
			System.out.println("Cannot predict this column: " + columnName);
			return null;
		}
		
		ArrayList<String> toReturn = new ArrayList<String>();
		for(String depend : dependArray){
			toReturn.add(depend);
		}
		
		return toReturn;
	}

	public void decompress(String tableName, String columnName,
			String predictFilesFolder, HashMap<String, Object> dependencies, String modelName)
			throws Exception {
		
		readHeader(tableName);
		
		Classifier classifier = null;

		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("select model from "
				+ tableName + " where attribute =  '" + columnName + "';");
		if (resultSet.first()) {
			InputStream in = resultSet.getBinaryStream(1);
			File file = new File("Temp");
			FileOutputStream fos = new FileOutputStream(file);
			byte[] data = new byte[4096];
			int count = -1;
			while ((count = in.read(data, 0, 4096)) != -1) {
				fos.write(data, 0, count);
			}
			fos.close();
			classifier = (Classifier) SerializeUtils.readFromFile(file);
		} else {
			System.out.println("SQL exception: No such table/colunmn.");
			return;
		}

		Instance predInstance = null;
		Instances resultInstances = null;


		if (dependencies.isEmpty()) {
			System.out.println("Cannot predict this column: " + columnName);
			return;
		}

		HashMap<String, Integer> dependIndex = new HashMap<String, Integer>();
		
		for (String column : dependencies.keySet()) {
			dependIndex.put(column, header.attribute(column).index());
		}



		Attribute resultAttribute = header.attribute(columnName);
		ArrayList<Attribute> resultAttrinfo = new ArrayList<Attribute>();
		resultAttrinfo.add(resultAttribute);
		resultInstances = new Instances(resultAttribute.name(), resultAttrinfo,
				1);



		predInstance = new DenseInstance(header.numAttributes());
		predInstance.setDataset(header);
		for (String column : dependencies.keySet()) {
			if (header.attribute(dependIndex.get(column)).isNumeric()) {
				double val;
				try {
					val = (Double)dependencies.get(column);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					break;
				}
				predInstance.setValue(dependIndex.get(column), val);
			} else {
				String val = (String)dependencies.get(column);
				predInstance.setValue(dependIndex.get(column), val);
			}
		}
			
		CustomWEKAClassifier classifier2 = (CustomWEKAClassifier) classifier;

		Instance instanceToAdd = new DenseInstance(1);

		instanceToAdd.setDataset(resultInstances);

		if (modelName.equals("M5P")) {

			M5P m5p = (M5P) (classifier2.getWEKAClassifier());

			if (header.attribute(columnName).isNumeric()) {

				instanceToAdd.setValue(0,
						m5p.classifyInstance(predInstance));
			} else {
				System.out.println("M5P can only predict numeric value.");
				return;
			}

		}

		else if (modelName.equals("REPTree")) {
			if (header.attribute(columnName).isNumeric()) {
				instanceToAdd.setValue(0,
						Utilities.numericValue(classifier, predInstance));
			} else {
				instanceToAdd.setValue(0,
						Utilities.nominalValue(classifier, predInstance));
			}
		}

		resultInstances.add(instanceToAdd);


		StringBuilder sb = new StringBuilder(predictFilesFolder);
		sb.append('/');
		sb.append(columnName);
		sb.append(".arff");
		ArffSaver saver = new ArffSaver();
		saver.setInstances(resultInstances);
		saver.setFile(new File(sb.toString()));
		saver.setDestination(new File(sb.toString()));
		saver.writeBatch();

		System.out
				.println("Decompression done! The predicted column is saved at '"
						+ predictFilesFolder + "' folder.");
	}
	
	public void decompress(String tableName, String columnName,
			String columnsFileFolder, String predictFilesFolder, String modelName)
			throws Exception {
		
		readHeader(tableName);
		
		Classifier classifier = null;

		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("select model from "
				+ tableName + " where attribute =  '" + columnName + "';");
		if (resultSet.first()) {
			InputStream in = resultSet.getBinaryStream(1);
			File file = new File("Temp");
			FileOutputStream fos = new FileOutputStream(file);
			byte[] data = new byte[4096];
			int count = -1;
			while ((count = in.read(data, 0, 4096)) != -1) {
				fos.write(data, 0, count);
			}
			fos.close();
			classifier = (Classifier) SerializeUtils.readFromFile(file);
		} else {
			System.out.println("SQL exception: No such table/colunmn.");
			return;
		}

		Instance predInstance = null;
		Instances resultInstances = null;

		resultSet = statement.executeQuery("select dependencies from "
				+ tableName + " where attribute =  '" + columnName + "';");

		String dependencies;
		if (resultSet.next()) {

			dependencies = resultSet.getString(1);
		} else {
			System.out.println("SQL exception: No such table/colunmn.");
			return;
		}

		String[] dependArray = dependencies.split(",");

		if (dependArray[0].equals("null")) {
			System.out.println("Cannot predict this column: " + columnName);
			return;
		}

		int[] dependIndex = new int[dependArray.length];
		for (int i = 0; i < dependArray.length; i++) {
			dependIndex[i] = header.attribute(dependArray[i]).index();
		}

		BufferedReader[] reader = new BufferedReader[dependArray.length];
		for (int i = 0; i < reader.length; i++) {
			reader[i] = new BufferedReader(new FileReader(columnsFileFolder
					+ "/" + dependArray[i] + ".arff"));
		}

		Instances[] instances = new Instances[dependArray.length];
		for (int i = 0; i < reader.length; i++) {
			instances[i] = new Instances(reader[i]);
		}

		Attribute resultAttribute = header.attribute(columnName);
		ArrayList<Attribute> resultAttrinfo = new ArrayList<Attribute>();
		resultAttrinfo.add(resultAttribute);
		resultInstances = new Instances(resultAttribute.name(), resultAttrinfo,
				instances[0].size());

		long time = System.nanoTime();
		
		for (int i = 0; i < instances[0].size(); i++) {

			predInstance = new DenseInstance(header.numAttributes());
			predInstance.setDataset(header);
			for (int j = 0; j < dependArray.length; j++) {
				if (header.attribute(dependIndex[j]).isNumeric()) {
					double val;
					try {
						val = instances[j].get(i).value(0);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						break;
					}
					predInstance.setValue(dependIndex[j], val);
				} else {
					String val = instances[j].get(i).stringValue(0);
					predInstance.setValue(dependIndex[j], val);
				}
			}
			
			CustomWEKAClassifier classifier2 = (CustomWEKAClassifier) classifier;

			Instance instanceToAdd = new DenseInstance(1);

			instanceToAdd.setDataset(resultInstances);

			if (modelName.equals("M5P")) {

				M5P m5p = (M5P) (classifier2.getWEKAClassifier());

				if (header.attribute(columnName).isNumeric()) {

					instanceToAdd.setValue(0,
							m5p.classifyInstance(predInstance));
				} else {
					System.out.println("M5P can only predict numeric value.");
					return;
				}

			}

			else if (modelName.equals("REPTree")) {
				if (header.attribute(columnName).isNumeric()) {
					instanceToAdd.setValue(0,
							Utilities.numericValue(classifier, predInstance));
				} else {
					instanceToAdd.setValue(0,
							Utilities.nominalValue(classifier, predInstance));
				}
			}

			resultInstances.add(instanceToAdd);
		}

		StringBuilder sb = new StringBuilder(predictFilesFolder);
		sb.append('/');
		sb.append(columnName);
		sb.append(".arff");
		ArffSaver saver = new ArffSaver();
		saver.setInstances(resultInstances);
		saver.setFile(new File(sb.toString()));
		saver.setDestination(new File(sb.toString()));
		saver.writeBatch();

		//ends
		if(MEASURE_TIME){
			time = System.nanoTime() - time;
			System.out.println("This prediction takes time: " + time);
		}
		
		System.out
				.println("Decompression done! The predicted column is saved at '"
						+ predictFilesFolder + "' folder.");
	}

	public static void main(String[] args) throws Exception {
		String tableName = args[0];
		String columnName = args[1];
		String columnFilesFolder = args[2];
		String predictFilesFolder = args[3];
		DecompressByDependency dbd = new DecompressByDependency();
		dbd.decompress(tableName, columnName, columnFilesFolder, predictFilesFolder, "REPTree");
		//HashMap<String, Object> map = new HashMap<String, Object>();
		//map.put("PRERELG", 0.0);
		//map.put("PTERN", -0.01);
		//dbd.decompress(tableName, columnName, predictFilesFolder, map, "REPTree");

	}

}