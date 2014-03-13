package Experiments;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;

import Online.Optimizer;

public class OptimizerTimeExperiment {

	static String query1 = "SELECT GEREG, GESTCEN FROM cps;";
	static String columnDir = "columns";
	static String optimizerOutputDir = "OptimizerOutput";
	
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
		FileUtils.deleteDirectory(new File(optimizerOutputDir));
		Optimizer optimizer = new Optimizer(connection, columnDir);
		long time1 = measureOptimizerTime(optimizer, query1);
		long time2 = measureNormalSQLTime(connection, query1);
		System.out.println("Optimizer time: " + time1);
		System.out.println("Regular Query time: " + time2);
		return;
	}
}
