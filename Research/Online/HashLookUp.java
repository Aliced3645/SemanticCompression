package Online;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;

public class HashLookUp {
	private Connection connection;
	
	private HashSet<HashMap<String, Double>> e_1 = new HashSet<HashMap<String, Double>>();
	private HashSet<HashMap<String, Double>> e_5 = new HashSet<HashMap<String, Double>>();
	private HashSet<HashMap<String, Double>> e_10 = new HashSet<HashMap<String, Double>>();
	private HashSet<HashMap<String, Double>> e_25 = new HashSet<HashMap<String, Double>>();
	
	public HashLookUp() throws SQLException {

		connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
	}
	
	public HashSet<HashMap<String, Double>> getHash(String tableName, String columnName, double errorBound) throws SQLException, IOException, ClassNotFoundException {
		
		String hashName = tableName + "_hash";
		
		if(errorBound == 0.01) {
			HashSet<HashMap<String, Double>> e_1 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_1 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_1 = (HashSet<HashMap<String, Double>>) x;
			}
			
			return e_1;
		}
		
		else if(errorBound == 0.05) {
			HashSet<HashMap<String, Double>> e_5 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_5 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_5 = (HashSet<HashMap<String, Double>>) x;
			}
			
			return e_5;
		}
		
		else if(errorBound == 0.1) {
			HashSet<HashMap<String, Double>> e_10 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_10 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_10 = (HashSet<HashMap<String, Double>>) x;
			}
			
			return e_10;
		}
		
		else if(errorBound == 0.25) {
			HashSet<HashMap<String, Double>> e_25 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_25 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_25 = (HashSet<HashMap<String, Double>>) x;
			}
			
			return e_25;
		}
		
		else return null;
	}
	
	public boolean LookUp(String tableName, String columnName, HashMap<String, Double> dependencies, double errorBound) throws SQLException, IOException, ClassNotFoundException {

		
		String hashName = tableName + "_hash";
		
		if(errorBound >= 0.01 && e_1.isEmpty()) {
			e_1 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_1 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_1 = (HashSet<HashMap<String, Double>>) x;
			}
		}
		
		if(errorBound >= 0.05 && e_5.isEmpty()) {
			e_5 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_5 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_5 = (HashSet<HashMap<String, Double>>) x;
			}
		}
		
		if(errorBound >= 0.1 && e_10.isEmpty()) {
			e_10 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_10 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_10 = (HashSet<HashMap<String, Double>>) x;
			}

		}

		if(errorBound >= 0.25 && e_25.isEmpty()) {
			e_25 = new HashSet<HashMap<String, Double>>();
			
			String query = "select e_25 from " + hashName + " where columnName = '" + columnName + "'";
			Statement  statement = connection.prepareStatement(query);

			ResultSet  resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				InputStream is = resultSet.getBlob(1).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object x = ois.readObject();
			    e_25 = (HashSet<HashMap<String, Double>>) x;
			}
		}

		
		return e_1.contains(dependencies) || e_5.contains(dependencies) || e_10.contains(dependencies) || e_25.contains(dependencies);
		
	}
	
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
		HashLookUp hl = new HashLookUp();
		
		HashMap<String, Double> test = new HashMap<String, Double>();
		test.put("P6p2", 0.049252);
		test.put("H18pA", 0.052246);
		test.put("P18p2", 0.003251);
		
		System.out.println(hl.LookUp("house_REPTree", "P1", test, 0.10));
		System.out.println(hl.LookUp("house_REPTree", "P1", test, 0.10));
		System.out.println(hl.LookUp("house_REPTree", "P1", test, 0.10));
	}

}