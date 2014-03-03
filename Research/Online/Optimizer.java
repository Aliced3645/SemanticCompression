package Online;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL optimizer.
 * In v1 let's assume select all columns from one table.
 * @author shu
 */
public class Optimizer {
	
	private Connection connection;
	
	public Optimizer(Connection connection){
		this.connection = connection;
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
	 */
	public List<List<String>> getColumnsPermutation(String sql){
		List<List<String>> permutations = new ArrayList<List<String>>();
		
		return permutations;
	}
	
	
}
