package MySQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

/**
 * Ingesting data to a very simple table: (A Integer, B Integer). The rule is
 * that values of column B is twice as the values of A.
 * 
 * @author shu
 */

public class DataGenerator {

	private static Connection connection = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;

	/**
	 * Initialization for database connections.
	 */
	static {
		try {
			connection = DriverManager
					.getConnection("jdbc:mysql://localhost/testdb?"
							+ "user=shu&password=shu");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Insert a desired size of rows.
	 * 
	 * @param size
	 * @throws SQLException 
	 */
	static void randomGeneration(int size) throws SQLException {
		//test if the table called 'table2' exists;
		statement = connection.createStatement();
		resultSet = statement.executeQuery(
				"show tables like 'table2'");
		if(resultSet.first()){
			//drop the table and start again.
			statement = connection.createStatement();
			statement.executeUpdate("drop table table2");
		}
		//create this new table
		statement.executeUpdate("create table table2 (A INTEGER, B INTEGER);");
		
		// insert data
		Random random = new Random();
		String insertSQL = "insert into table2 values (?, ?)";
		for(int i = 0; i < size; i ++){
			int a = random.nextInt(100);
			int b = a * 2;
			PreparedStatement pstatement = connection.prepareStatement(insertSQL);
			pstatement.setInt(1, a);
			pstatement.setInt(2, b);
			pstatement.executeUpdate();
		}
		
	}

	public static void main(String[] args) throws SQLException {
		randomGeneration(10000);
	}
}
