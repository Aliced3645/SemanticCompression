package Online;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import com.google.common.io.Files;

import Zql.ParseException;

/**
 * SQL optimizer. In v1 let's assume select all columns from one table.
 * 
 * @author shu
 */
public class Optimizer {

	private Connection connection;
	SQLParser parser = new SQLParser();
	//String columnsFolderPath;
	DecompressByDependency decompressor;

	String[] modelTypes = { "REPTree", "M5P" };

	public Optimizer(Connection connection)
			throws SQLException {
		this.connection = connection;
		//this.columnsFolderPath = columnsFolder;
		decompressor = new DecompressByDependency();
		decompressor.setConnection(connection);
	}

	// Helper functions
	void recursive(List<List<String>> answers, List<String> answer,
			boolean[] used, List<String> strings) {
		if (answer.size() == strings.size()) {
			answers.add(new ArrayList<String>(answer));
			return;
		}

		for (int i = 0; i < used.length; i++) {
			if (used[i] == false) {
				used[i] = true;
				answer.add(strings.get(i));
				recursive(answers, answer, used, strings);
				answer.remove(answer.size() - 1);
				used[i] = false;
			}
		}
	}

	public List<List<String>> getPermutations(List<String> strings) {
		List<List<String>> answers = new ArrayList<List<String>>();
		if (strings == null)
			return answers;
		List<String> answer = new ArrayList<String>();
		boolean[] used = new boolean[strings.size()];
		Arrays.fill(used, false);
		recursive(answers, answer, used, strings);
		return answers;
	}

	/**
	 * 1. Get columns from the sql. 2. Get all possible read orders for these
	 * tables, eg: select a,b,c from table So we get: a, b, c a, c, b b, a, c
	 * ..... etc.
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 * 
	 *             For now, not dealing with "*" case.
	 */
	private List<List<String>> getColumnsPermutations(String sql)
			throws ParseException {
		List<List<String>> permutations = new ArrayList<List<String>>();
		Hashtable<String, List<String>> parts = parser.parseSQL(sql);
		List<String> columns = parts.get("columns");
		return getPermutations(columns);
	}

	/**
	 * Helper function. Read column from the directory and put to destination.
	 * 
	 * @param columnName
	 * @throws IOException
	 */
	private void readColumnDirectly(String tableName, String columnName, String outputDir)
			throws IOException {
		String path = tableName + "/" + columnName + ".arff";
		String outputPath = outputDir + "/" + columnName + ".arff";
		File from = new File(path);
		File to = new File(outputPath);
		Files.copy(from, to);
	}

	/**
	 * Initial version of getting all possible results basing on columns
	 * permutations;
	 * 
	 * @param sql
	 * @throws Exception
	 */
	private void produceAllPossibleResults(String sql, String modelType) throws Exception {
		List<List<String>> permutations = getColumnsPermutations(sql);

		if (permutations.size() == 0) {
			System.out.println("Error getting permutations");
			return;
		}

		// for now, only support one table in SQL.
		String table = parser.parseTables(sql).get(0);
		String metadataTable = table + '_' + modelType;

		// the "*" case
		if (permutations.size() == 1 && permutations.get(0).get(0) == "*") {
			// read all columns of this table;
			Statement statement = connection.createStatement();
			String sqlGetCoumns = "select attribute from " + metadataTable + ";";
			ResultSet resultSet = statement.executeQuery(sqlGetCoumns);
			List<String> allColumns = new LinkedList<String>();
			while (resultSet.next()) {
				allColumns.add(resultSet.getString(1));
			}
			permutations = this.getPermutations(allColumns);
		}

		decompressor.setConnection(connection);

		String baseDir = "OptimizerOutput";
		// for each possible permutations, and each model.

			for (List<String> possibility : permutations) {

				HashMap<String, Boolean> queryColumnsSet = new HashMap<String, Boolean>();

				for (String column : permutations.get(0)) {
					queryColumnsSet.put(column, false);
				}

				// Make the name of output dir
				StringBuilder sb = new StringBuilder();
				sb.append(baseDir);
				sb.append("/");

				for (String column : possibility) {
					sb.append(column);
					sb.append("_");
				}
				sb.append(modelType);

				String outputDir = sb.toString();

				(new File(outputDir)).mkdirs();

				// go for each column from the first to the last one.
				for (int i = 0; i < possibility.size(); i++) {
					String column = possibility.get(i);

					if (queryColumnsSet.containsKey(column)) {
						if (queryColumnsSet.get(column) == true)
							continue;
					}

					List<String> dependencies = decompressor.getDependencies(
							metadataTable, column);
					// see if all dependencies are in the columns array.
					boolean readDirectly = false;
					if (dependencies == null || dependencies.size() == 0) {
						readDirectly = true;
					} else {
						for (String dependency : dependencies) {
							if (!queryColumnsSet.containsKey(dependency)) {
								readDirectly = true;
							}
						}
					}
					if (readDirectly) {
						this.readColumnDirectly(table, column, outputDir);
					} else {
						// TODO: TO BE IMPROVED. Still not making sense though.
						// read other dependent columns first.
						for (String dependency : dependencies) {
							if (queryColumnsSet.containsKey(dependency)) {
								if (queryColumnsSet.get(dependency) == false) {
									readColumnDirectly(table, dependency, outputDir);
									queryColumnsSet.put(dependency, true);
								}
							}
						}
						decompressor.decompress(metadataTable, column,
								table, outputDir, modelType);
					}
				}
			}
		

	}

	/**
	 * Validating the where part meets our condition.
	 * 
	 * @throws ParseException
	 * @throws SQLException
	 * 
	 */
	private boolean validateWhere(String sql) throws ParseException,
			SQLException {
		if (!parser.hasWhere(sql))
			return false;
		HashMap<String, Object> where = parser.parseWhere(sql);
		// only for the first column
		String column = parser.parseColumns(sql).get(0);
		String table = parser.parseTables(sql).get(0);
		// Get dependency
		List<String> dependencies = decompressor.getDependencies(table, column);
		// validate
		for (String attribute : where.keySet()) {
			if (!dependencies.contains(attribute))
				return false;
		}
		return true;
	}

	/**
	 * Having WHERE and meet out requirement.
	 * 
	 * @param sql
	 * @throws Exception 
	 */
	private void processQueryWithWhere(String sql, String modelType) throws Exception {
		HashMap<String, Object> wheres = parser.parseWhere(sql);
		// for now, only support one table in SQL.
		String table = parser.parseTables(sql).get(0) + '_' + modelType;
		String column = parser.parseColumns(sql).get(0);
		String baseDir = "OptimizerOutput/WherePredict/";
		baseDir += modelType;
		decompressor.decompress(table, column, baseDir, wheres, modelType);
	}

	/**
	 * 
	 * ***************************************** 
	 * The general function for
	 * processing SQL query. 
	 * 
	 * Two branches: 1. Having WHERE and meet our
	 * requirements  2. Permute all possible
	 * *****************************************
	 * 
	 * @param sql
	 * @throws Exception
	 */
	public void processQuery(String sql) throws Exception {
		if (validateWhere(sql)) {
			for(String model : modelTypes)
				processQueryWithWhere(sql, model);
		} else {
			for(String model : modelTypes)
				produceAllPossibleResults(sql, model);
		}
	}

	
	/**
	 * Another version of processing query, process using a specified model.
	 */
	public void processQuery(String sql, String model) throws Exception {
		if(validateWhere(sql)){
			processQueryWithWhere(sql, model);
		} else {
			produceAllPossibleResults(sql, model);
		}
	}
	
	
	/**
	 * Running simple tests
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Connection connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
		
		Optimizer optimizer = new Optimizer(connection);
		optimizer.decompressor.getDependencies("cps", "GEREG");
		String sql = "SELECT GEREG, GESTCEN FROM cps;";
		//String sql = "SELECT GEREG FROM FROM cps WHERE "
		optimizer.processQuery(sql);
	}
}
