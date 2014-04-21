package Offline;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;

import weka.classifiers.trees.M5P;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import WekaTraining.CustomWEKAClassifier;
import WekaTraining.Utilities;
import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;

public class HashStore {
	
	private Connection connection;
	
	InstancesHeader header;
	
	BufferedReader inputStream;
	
	HashSet<HashMap<String, Double>> e_1;
	HashSet<HashMap<String, Double>> e_5;
	HashSet<HashMap<String, Double>> e_10;
	HashSet<HashMap<String, Double>> e_25;

	
	public HashStore() throws SQLException {

		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
		
		e_1 = new HashSet<HashMap<String, Double>>();
		e_5 = new HashSet<HashMap<String, Double>>();
		e_10 = new HashSet<HashMap<String, Double>>();
		e_25 = new HashSet<HashMap<String, Double>>();
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
	
	public void store(String tableName, String columnName,
			String columnFilesFolder, String modelName)
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
			reader[i] = new BufferedReader(new FileReader(columnFilesFolder
					+ "/" + dependArray[i] + ".arff"));
		}

		Instances[] instances = new Instances[dependArray.length];
		for (int i = 0; i < reader.length; i++) {
			instances[i] = new Instances(reader[i]);
		}
		
		BufferedReader t_reader = new BufferedReader(new FileReader(columnFilesFolder
					+ "/" + columnName + ".arff"));
		
		Instances t_instances = new Instances(t_reader);

		
		for (int i = 0; i < instances[0].size(); i++) {

			predInstance = new DenseInstance(header.numAttributes());
			
			header.setClassIndex(header.numAttributes()-1);
			predInstance.setDataset(header);
			
			HashMap<String, Double> hashKey = new HashMap<String, Double>();
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
					hashKey.put(dependArray[j], val);
				} else {
					System.out.println("HashStore support numeric value only.");
					return;
				}
			}
			
			CustomWEKAClassifier classifier2 = (CustomWEKAClassifier) classifier;


			if (modelName.equals("M5P")) {

				M5P m5p = (M5P) (classifier2.getWEKAClassifier());

				if (header.attribute(columnName).isNumeric()) {

							
							
							double srcValue = t_instances.get(i).value(0);
							double predValue = m5p.classifyInstance(predInstance);
							double diff = srcValue - predValue;
							
							double result = srcValue == 0 ? Math.abs(diff) : Math.abs(diff / srcValue);
							
							
							if(result < 0.01) {
								e_1.add(hashKey);
								
							}
							
							else if(result >= 0.01 && result < 0.05) {
								e_5.add(hashKey);
							}
							
							else if(result >= 0.05 && result < 0.1) {
								e_10.add(hashKey);				
							}
							
							else if(result >= 0.1 && result < 0.25) {
								e_25.add(hashKey);
							}
				} else {
					System.out.println("HashStore support numeric value only.");
					return;
				}

			}

			else if (modelName.equals("REPTree")) {
				if (header.attribute(columnName).isNumeric()) {
					
							
							double srcValue = t_instances.get(i).value(0);
							double predValue = 	Utilities.numericValue(classifier, predInstance);
							double diff = srcValue - predValue;
							
							double result = srcValue == 0 ? Math.abs(diff) : Math.abs(diff / srcValue);
							
							
							if(result < 0.01) {
								e_1.add(hashKey);
								
							}
							
							else if(result >= 0.01 && result < 0.05) {
								e_5.add(hashKey);
							}
							
							else if(result >= 0.05 && result < 0.1) {
								e_10.add(hashKey);				
							}
							
							else if(result >= 0.1 && result < 0.25) {
								e_25.add(hashKey);
							}
				} else {
					
					System.out.println("HashStore support numeric value only.");
					return;
				}
			}

		}
		storeToDB(tableName, columnName);

	}
	
	private void storeToDB(String tableName, String columnName) throws SQLException {
		String hashName = tableName + "_hash";
		
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("show tables like '" + hashName + "'");
		if (!resultSet.first()) {
			statement = connection.createStatement();
			statement
					.executeUpdate("create table " + hashName + " (columnName CHAR(50), e_1 LONGBLOB, e_5 LONGBLOB, e_10 LONGBLOB, e_25 LONGBLOB);");
			statement = connection.createStatement();
			statement
					.executeUpdate("alter table " + hashName + " ADD PRIMARY KEY (columnName);");
		}
		
		ResultSet resultSet2 = statement.executeQuery("select * from " + hashName + " where columnName = '" + columnName + "'");
		
		if (!resultSet2.first()) {
		
			String sql = "Insert into " + hashName + " (columnName, e_1, e_5, e_10, e_25) values (?, ?, ?, ?, ?)";
		
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, columnName);
			ps.setObject(2, (Object) e_1);
			ps.setObject(3, (Object) e_5);
			ps.setObject(4, (Object) e_10);
			ps.setObject(5, (Object) e_25);
			ps.execute();
			ps.close();
		
		}
		
	}
	
	
	public void test() throws SQLException, IOException, ClassNotFoundException {
		
		HashMap<String, Double> test = new HashMap<String, Double>();
		test.put("P6p2", 0.049252);
		test.put("H18pA", 0.052246);
		test.put("P18p2", 0.003251);
		
		HashSet<HashMap<String, Double>> testSet = null;
		
		String query = "select e_10 from house_REPTree_hash where columnName = 'P1'";
		Statement  statement = connection.prepareStatement(query);

		ResultSet  resultSet = statement.executeQuery(query);
		if (resultSet.next()) {
			InputStream is = resultSet.getBlob(1).getBinaryStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			Object x = ois.readObject();
		    testSet = (HashSet<HashMap<String, Double>>) x;
		}
		System.out.println(testSet.contains(test));
	}
	
	public static void main(String[] args) throws Exception {
		
		String tableName = args[0];
		String columnName = args[1];
		String columnFilesFolder = args[2];
		
		HashStore hs = new HashStore();
		hs.store(tableName, columnName, columnFilesFolder, "REPTree");
		
		System.out.println(hs.e_1.size());
		System.out.println(hs.e_5.size());
		System.out.println(hs.e_10.size());
		System.out.println(hs.e_25.size());
		
		hs.test();
		
		/*HashMap<String, Double> test = new HashMap<String, Double>();
		test.put("P6p2", 0.049252);
		test.put("H18pA", 0.052246);
		test.put("P18p2", 0.003251);
		


		
		System.out.println(hs.e_1.contains(test));
		System.out.println(hs.e_5.contains(test));
		System.out.println(hs.e_10.contains(test));
		System.out.println(hs.e_25.contains(test));*/
	}

}