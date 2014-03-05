package Online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import Zql.ParseException;

/**
 * SQL optimizer.
 * In v1 let's assume select all columns from one table.
 * @author shu
 */
public class Optimizer {
	
	private Connection connection;
	SQLParser parser = new SQLParser();
	String columnsFolderPath;
	
	public Optimizer(Connection connection, String columnsFolder){
		this.connection = connection;
		this.columnsFolderPath = columnsFolder;
	}
	
	
	// Helper functions
	void recursive(List<List<String>> answers, List<String> answer, 
			boolean[] used, List<String> strings){
		if(answer.size() == strings.size()){
			answers.add(new ArrayList<String>(answer));
			return;
		}
		
		for(int i = 0; i < used.length; i ++) {
			if(used[i] == false){
				used[i] = true;
				answer.add(strings.get(i));
				recursive(answers, answer, used, strings);
				answer.remove(answer.size() - 1);
				used[i] = false;
			}
		}
	}
	
	public List<List<String>> getPermutations(List<String> strings){
		List<List<String>> answers = new ArrayList<List<String>>();
		if(strings == null) return answers;
		List<String> answer = new ArrayList<String>();
		boolean[] used = new boolean[strings.size()];
		Arrays.fill(used, false);
		recursive(answers, answer, used, strings);
		return answers;
	}
	
	/**
	 * 1. Get columns from the sql.
	 * 2. Get all possible read orders for these tables, eg:
	 * 	select a,b,c from table
	 * So we get:
	 * a, b, c
	 * a, c, b
	 * b, a, c
	 * ..... etc.
	 * @param sql
	 * @return
	 * @throws ParseException 
	 * 
	 * For now, not dealing with "*" case.
	 */
	private List<List<String>> getColumnsPermutations(String sql) throws ParseException{
		List<List<String>> permutations = new ArrayList<List<String>>();
		Hashtable<String, List<String>> parts = parser.parseSQL(sql);
		List<String> columns = parts.get("columns");
		return getPermutations(columns);
	}
	
	
	/**
	 * 
	 * @param sql
	 * @throws ParseException 
	 */
	public void produceAllPossibleResults(String sql) throws ParseException{
		List<List<String>> permutations = getColumnsPermutations(sql);
		//for each possible permutations
		for(List<String> possibility : permutations){
			String[] columns = (String[]) possibility.toArray();
			//Make the name of output dir
			StringBuilder sb = new StringBuilder();
			for (String column : columns){
				sb.append(column);
				sb.append("_");
			}
			sb.deleteCharAt(sb.length() - 1);
			
			
		}
		
	}
	
	/**
	 * Running simple tests
	 * @param args
	 * @throws ParseException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws ParseException, SQLException{
		Connection connection = DriverManager
				.getConnection("jdbc:mysql://localhost/metadata?"
						+ "user=shu&password=shu");
		Optimizer optimizer = new Optimizer(connection, "columns");
		//String sql = "SELECT FirstName, LastName, Address, City, State FROM EmployeeAddressTable;";
		String sql2 = "SELECT * FROM EmployeeAddressTable;";
		System.out.println(optimizer.getColumnsPermutations(sql2));
	}
}
