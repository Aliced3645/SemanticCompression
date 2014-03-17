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

import Zql.ParseException;

import Online.Optimizer;
import Online.SQLParser;

public class OptimizerTimeExperiment {

	static String query1 = "SELECT GESTCEN FROM cps;";
	static String query2 = "SELECT GESTCEN FROM cps where "
			+ "GEREG = 3 AND GTCSA = 0 AND GTMETSTA = 2 AND HRHHID2 = 90001;";
	static String optimizerOutputDir = "OptimizerOutput";
	static String normalOutputDir = "NormalOutput";

	static SQLParser parser = new SQLParser();

	static String[] modelTypes = { "M5P", "REPTree" };

	static long measureOptimizerTime(Optimizer optimizer, String sql,
			String model) throws Exception {
		List<Long> times = new LinkedList<Long>();
		for(String modelType : modelTypes){
			List<List<String>> permutations = optimizer.getColumnsPermutations(sql, modelType);
			for(List<String> permutation : permutations) {
				//timing here
				long startTime = System.nanoTime();
				optimizer.queryOnePossibleResult(sql, permutation, "OptimizerOutput", modelType);
				long period = System.nanoTime() - startTime;
				times.add(period);
			}
		}
		//finally return the average
		int count = times.size();
		long total = 0;
		for(Long time : times) {
			total += time;
		}
		return total / count;
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
			String path = table + "/" + column + ".arff";
			String outputPath = normalOutputDir + "/" + column + ".arff";
			File from = new File(path);
			File to = new File(outputPath);
			Files.copy(from, to);
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
		double time1 = measureOptimizerTimeForWhere(optimizer, query2, "M5P");
		System.out.println("Optimizer Query time: " + time1);
		double time2 = measureNormalReadTimeForWhere(connection, query2);
		System.out.println("Regular Query time: " + time2);
		System.out.println("Optimizer is " + (time2 / time1) + " times faster");
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
		// testWhere(connection, optimizer);

		// For normal queries.
		for (String model : modelTypes) {
			long time1 = measureOptimizerTime(optimizer, query1, model);
			System.out.println(model + " : " + time1);
		}

		long time2 = measureNormalReadTime(connection, query1);
		System.out.println("Regular Query time: " + time2);

		return;
	}
}
