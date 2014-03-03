package Online;

import java.sql.Connection;
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
	
	public Optimizer(Connection connection){
		this.connection = connection;
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
	 */
	public List<List<String>> getColumnsPermutations(String sql) throws ParseException{
		List<List<String>> permutations = new ArrayList<List<String>>();
		Hashtable<String, List<String>> parts = parser.parseSQL(sql);
		List<String> columns = parts.get("columns");
		return getPermutations(columns);
	}
	
	/**
	 * Running simple tests
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException{
		Optimizer optimizer = new Optimizer(null);
		String sql = "SELECT FirstName, LastName, Address, City, State FROM EmployeeAddressTable;";
		System.out.println(optimizer.getColumnsPermutations(sql));
	}
}
