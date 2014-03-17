package Experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import weka.core.Instance;
import weka.core.Instances;

import com.google.common.io.Files;

import Utilities.ColumnReader;
import Zql.ParseException;

import Online.DecompressByDependency;
import Online.Optimizer;
import Online.SQLParser;

public class OptimizerTimeExperiment {

	static String query1 = "SELECT GESTCEN, GEREG FROM cps;";
	static String query2 = "SELECT GESTCEN FROM cps where "
			+ "GEREG = 3 AND GTCSA = 0 AND GTMETSTA = 2 AND HRHHID2 = 90001;";
	static String query3 = "SELECT GESTCEN,GEREG,GTCSA,GTMETSTA, HRHHID2 FROM cps;";
	static String query4 = "SELECT PRERELG, PTWK, PTERN FROM cps;";
	static String query5 = "SELECT PRERELG FROM cps where PTWK = 0 AND PTERN = 0.01;";
	
	static String optimizerOutputDir = "OptimizerOutput";
	static String normalOutputDir = "NormalOutput";

	static SQLParser parser = new SQLParser();

	static String[] modelTypes = {"REPTree" };

	static long measureOptimizerTime(Optimizer optimizer, String sql,
			String model) throws Exception {
		List<Long> times = new LinkedList<Long>();
		List<List<String>> permutations = optimizer.getColumnsPermutations(sql,
				model);
		for (List<String> permutation : permutations) {
			// timing here
			long startTime = System.nanoTime();
			optimizer.queryOnePossibleResult(sql, permutation,
					"OptimizerOutput", model);
			long period = System.nanoTime() - startTime;
			times.add(period);
		}
		
		// finally return the average
		int count = times.size();
		long total = 0;
		for (Long time : times) {
			total += time;
		}
		return total / count;
	}
	
	static long measureOptimizerTimeInSpecificOrder(Optimizer optimizer, List<String> permutation, String sql, 
			String model) throws Exception{
		long startTime = System.nanoTime();
		optimizer.queryOnePossibleResult(sql, permutation,
				"OptimizerOutput", model);
		long period = System.nanoTime() - startTime;
		return period;
	}

	/**
	 * Read directly from the disk and store the result to the destination.
	 * 
	 * @param connection
	 * @param query
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 * @throws IOException
	 */
	static long measureNormalReadTime(Connection connection, String query)
			throws SQLException, ParseException, IOException {
		long startTime = System.nanoTime();

		List<String> columns = parser.parseColumns(query);
		List<String> tables = parser.parseTables(query);
		String table = tables.get(0);

		if (columns.size() == 1 && columns.get(0).equals("*")) {
			Statement statement = connection.createStatement();
			String sqlGetCoumns = "select attribute from " + table + '_'
					+ "REPTree" + ";";
			ResultSet resultSet = statement.executeQuery(sqlGetCoumns);
			columns = new LinkedList<String>();
			while (resultSet.next()) {
				columns.add(resultSet.getString(1));
			}
		}
		// Read all and store into new files..
		for (String column : columns) {
			String outputPath = normalOutputDir + "/" + column + ".arff";
			ColumnReader.readAndWrite(table, column, outputPath);
		}

		return System.nanoTime() - startTime;
	}

	/**
	 * Now only for numeric contents, brute-force query on where. Method: For
	 * columns after WHERE, read their column files and find satisfying row
	 * index. Then do a intersection of these indices so we can get the indice
	 * of satisfying rows of the SELECT row.
	 * 
	 * @param connection
	 * @param query
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	static long measureNormalReadTimeForWhere(Connection connection,
			String query) throws ParseException, IOException {

		long startTime = System.nanoTime();
		HashMap<String, Object> wheres = parser.parseWhere(query);
		String columnDir = parser.parseTables(query).get(0);
		List<Set<Integer>> sets = new LinkedList<Set<Integer>>();
		for (String column : wheres.keySet()) {
			String columnFile = columnDir + "/" + column + ".arff";
			Set<Integer> set = new HashSet<Integer>();
			BufferedReader reader = new BufferedReader(new FileReader(
					columnFile));
			Instances instances = new Instances(reader);
			reader.close();
			for (int i = 0; i < instances.size(); i++) {
				Instance instance = instances.get(i);
				if (instance.value(0) == (Double) wheres.get(column)) {
					set.add(i);
				}
			}
			sets.add(set);
		}
		Set<Integer> firstSet = sets.get(0);
		for (int i = 1; i < sets.size(); i++) {
			firstSet.retainAll(sets.get(i));
		}

		// The firstSet only contains the intersection of all sets of row
		// indices.
		// Now get satisfying answers.
		List<Double> answers = new LinkedList<Double>();
		String selectColumn = parser.parseColumns(query).get(0);
		String columnFile = columnDir + "/" + selectColumn + ".arff";
		BufferedReader reader = new BufferedReader(new FileReader(columnFile));
		Instances instances = new Instances(reader);
		reader.close();
		for (int i = 0; i < instances.size(); i++) {
			if (firstSet.contains(i)) {
				Instance instance = instances.get(i);
				answers.add(instance.value(0));
			}

		}
		return System.nanoTime() - startTime;
	}

	static long measureOptimizerTimeForWhere(Optimizer optimizer, String query,
			String model) throws Exception {
		long startTime = System.nanoTime();
		optimizer.processQuery(query, model);
		return System.nanoTime() - startTime;
	}

	static void testWhere(Connection connection, Optimizer optimizer)
			throws Exception {
		double time1 = measureOptimizerTimeForWhere(optimizer, query5, "REPTree");
		System.out.println("Optimizer Query time: " + time1);
		double time2 = measureNormalReadTimeForWhere(connection, query5);
		System.out.println("Regular Query time: " + time2);
		System.out.println("Optimizer is " + (time2 / time1) + " times faster");
	}

	public static void measureBasicTwoTypesOfReading(String tableName,
			String columnName) throws IOException {
		long time = System.nanoTime();
		ColumnReader.copyDirectly(tableName, columnName, "test");
		time = System.nanoTime() - time;
		System.out.println("Copy time: " + time);

		time = System.nanoTime();
		ColumnReader.readAndWrite(tableName, columnName, "test");
		time = System.nanoTime() - time;
		System.out.println("Read write time: " + time);
	}

	public static void measurePredictTimeAndReadWriteTime(String tableName,
			String columnName, DecompressByDependency decompressor)
			throws Exception {
		decompressor.decompress(tableName, columnName, "cps", "test", "M5P");
		long time = System.nanoTime();
		ColumnReader.readAndWrite("cps", columnName, "test");
		time = System.nanoTime() - time;
		System.out.println("Read write time: " + time);
	}

	// | GESTCEN | GEREG,GTCSA,GTMETSTA,HRHHID2
	public static void main(String[] args) throws Exception {
		Connection connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");

		FileUtils.deleteDirectory(new File(optimizerOutputDir));
		FileUtils.deleteDirectory(new File(normalOutputDir));
		(new File(normalOutputDir)).mkdirs();
		Optimizer optimizer = new Optimizer(connection);
		DecompressByDependency decompressor = new DecompressByDependency();
		decompressor.setConnection(connection);
		//measurePredictTimeAndReadWriteTime("cps_M5P", "GEREG", decompressor);
		testWhere(connection, optimizer);
		// measureBasicTwoTypesOfReading("cps", "GEREG");

		// For normal queries.
		
		LinkedList<String> permutation = new LinkedList<String>();
		permutation.add("PRERELG");
		permutation.add("PTWK");
		permutation.add("PTERN");
		//long time1 = measureOptimizerTimeInSpecificOrder(optimizer, permutation, query4, "REPTree");
		//long time1 = measureOptimizerTime(optimizer, query4, "REPTree");
		//System.out.println("REPTree" + " : " + time1);
		
		//long time2 = measureNormalReadTime(connection, query4);
		//System.out.println("Regular Query time: " + time2);
		
		return;
	}
}
