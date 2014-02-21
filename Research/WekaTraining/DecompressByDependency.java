package WekaTraining;


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


import WekaTraining.Utilities;
import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class DecompressByDependency {
	private Connection connection;

	InstancesHeader header;

	BufferedReader inputStream;
	
	public DecompressByDependency() throws SQLException {

		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	public void readHeader(String tableName) throws SQLException, IOException, ClassNotFoundException {
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement
				.executeQuery("select header from headers where name =  '" + tableName + "';");

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
	
	public void decompress(String tableName, String columnName, String columnsFileFolder, String predictFileFolder) throws SQLException, IOException, ClassNotFoundException {
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
		}

		Instance predInstance = null;
		Instances resultInstances = null;

		
		resultSet = statement.executeQuery("select dependencies from "
				+ tableName + " where attribute =  '" + columnName + "';");
		resultSet.next();

		String dependencies = resultSet.getString(1);
		
		String[] dependArray = dependencies.split(",");
		
		if(dependArray[0].equals("null")) {
			System.out.println("Cannot predict this column: " + columnName);
			return;
		}

		
		int[] dependIndex = new int[dependArray.length];
		for(int i = 0; i < dependArray.length; i++) {
			dependIndex[i] = header.attribute(dependArray[i]).index();
		}
		
		BufferedReader[] reader = new BufferedReader[dependArray.length];
		for(int i = 0; i < reader.length; i++) {
			reader[i] = new BufferedReader(new FileReader(columnsFileFolder + "/" + dependArray[i] + ".arff"));
		}
		
		Instances[] instances = new Instances[dependArray.length];
		for(int i = 0; i < reader.length; i++) {
			instances[i] = new Instances(reader[i]);
		}
		
		
		Attribute resultAttribute = header.attribute(columnName);
		ArrayList<Attribute> resultAttrinfo = new ArrayList<Attribute>();
		resultAttrinfo.add(resultAttribute);
		resultInstances = new Instances(resultAttribute.name(), resultAttrinfo, instances[0].size());
		
		for(int i = 0; i < instances[0].size(); i ++) {
			predInstance = new DenseInstance(header.numAttributes());
			predInstance.setDataset(header);
			for(int j = 0; j < dependArray.length; j++) {
				if(header.attribute(dependIndex[j]).isNumeric()) {
					double val;
					try {
                        val = instances[j].get(i).value(0);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        break;
                    }
					predInstance.setValue(dependIndex[j], val);
				}
				else {
					String val = instances[j].get(i).stringValue(0);
					predInstance.setValue(dependIndex[j], val);
				}
			}

			Instance instanceToAdd = new DenseInstance(1);
			instanceToAdd.setValue(0, Utilities.numericValue(classifier, predInstance));
			resultInstances.add(instanceToAdd);
		}
		
		StringBuilder sb = new StringBuilder(predictFileFolder);
		sb.append('/');
		sb.append(columnName);
		sb.append(".arff");
		ArffSaver saver = new ArffSaver();
		saver.setInstances(resultInstances);
		saver.setFile(new File(sb.toString()));
		saver.setDestination(new File(sb.toString()));
		saver.writeBatch();

	}
	
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
		String tableName = args[0];
		String columnName = args[1];
		String columnFilesFolder = args[2];
		String predictFileFolder = args[3];
		DecompressByDependency dbd = new DecompressByDependency();
		dbd.readHeader(tableName);
		dbd.decompress(tableName, columnName, columnFilesFolder, predictFileFolder);
		System.out.println("Decompression done! The predicted column is saved at '" + predictFileFolder + "' folder.");
	}
	
}