package Experiments;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import Online.Optimizer;

public class OptimizerTimeExperiment {

	static String query1 = "SELECT GEREG, GESTCEN FROM cps;";
	
	static long measureOptimizerTime(Optimizer optimizer, String query) throws Exception{
		long startTime = System.nanoTime();    
		optimizer.processQuery(query);
		return System.nanoTime() - startTime;
	}
	
	static long measureNormalSQLTime(Connection connection, String query) throws SQLException {
		long startTime = System.nanoTime();    
		Statement statement = connection.createStatement();
		statement.executeQuery(query);
		return System.nanoTime() - startTime;
	}
	
	
	public static void main(String[] args) throws Exception {
		Connection connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
		Optimizer optimizer = new Optimizer(connection, "columns");
		long time1 = measureOptimizerTime(optimizer, query1);
		long time2 = measureNormalSQLTime(connection, query1);
		
		return;
	}
}
