package MySQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Used for testing MySQL JDBC integration.
 * 
 * @author shu
 * 
 */
public class Tester {

	private static Connection connection = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;

	public static void main(String[] args) {
		try {
			connection = DriverManager.getConnection("jdbc:mysql://localhost/metadata?"
              + "user=shu&password=shu");
			statement = connection.createStatement();
			/*
			resultSet = statement.executeQuery(
					"select * from testdb.table1");
			while(resultSet.next()){
				System.out.println(resultSet.getInt("id"));
			}
			*/
			/*
			 * statement.executeUpdate(" insert into table3 (a) values (1);");
			 */
			try{
				//Test loading binary content from database and recover to weka objects.
				//ResultSet resultSet = statement.executeQuery("select ")
				//statement.executeQuery()
			} catch (Exception e){
				//delete table..
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}

				if (statement != null) {
					statement.close();
				}

				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {

			}
		}
	}
}
