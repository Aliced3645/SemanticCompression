package Experiments;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import Zql.ParseException;

import Online.Optimizer;
import Online.SQLParser;

public class OptimizerTimeExperiment {

	static String query1 = "SELECT GESTCEN FROM cps;";
	static String optimizerOutputDir = "OptimizerOutput";
	static String normalOutputDir = "NormalOutput";
	
	static SQLParser parser = new SQLParser();
	
	static String[] modelTypes = { "M5P", "REPTree" };
	
	static long measureOptimizerTime(Optimizer optimizer, String query, String model) throws Exception{
		long startTime = System.nanoTime();    
		optimizer.processQuery(query, model);
		return System.nanoTime() - startTime;
	}
	
	/**
	 * Read directly from the disk and store the result to the destination.
	 * @param connection
	 * @param query
	 * @return
	 * @throws SQLException
	 * @throws ParseException 
	 * @throws IOException 
	 */
	static long measureNormalReadTime(Connection connection, String query) throws SQLException, ParseException, IOException {
		long startTime = System.nanoTime();    
		
		List<String> columns = parser.parseColumns(query);
		List<String> tables = parser.parseTables(query);
		String table = tables.get(0);
		
		if(columns.size() == 1 && columns.get(0).equals("*")){
			Statement statement = connection.createStatement();
			String sqlGetCoumns = "select attribute from " + table + '_' + "REPTree" + ";";
			ResultSet resultSet = statement.executeQuery(sqlGetCoumns);
			columns = new LinkedList<String>();
			while (resultSet.next()) {
				columns.add(resultSet.getString(1));
			}
		}
		//Read all and store into new files..
		for(String column : columns){
			String path = table + "/" + column + ".arff";
			String outputPath = normalOutputDir + "/" + column + ".arff";
			File from = new File(path);
			File to = new File(outputPath);
			Files.copy(from, to);
		}
		
		return System.nanoTime() - startTime;
	}
	
	
	public static void main(String[] args) throws Exception {
		Connection connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
		
		FileUtils.deleteDirectory(new File(optimizerOutputDir));
		FileUtils.deleteDirectory(new File(normalOutputDir));
		(new File(normalOutputDir)).mkdirs();
		
		Optimizer optimizer = new Optimizer(connection);
		for(String model : modelTypes){
			long time1 = measureOptimizerTime(optimizer, query1, model);
			System.out.println(model + " : " + time1);
		}
		
		long time2 = measureNormalReadTime(connection, query1);
		System.out.println("Regular Query time: " + time2);
		
		return;
	}
}
