package Online;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import Zql.ParseException;
import Zql.ZExp;
import Zql.ZExpression;
import Zql.ZFromItem;
import Zql.ZQuery;
import Zql.ZSelectItem;
import Zql.ZqlParser;

/**
 * Given a SQL *query* sentence, 
 * try to parse it into different parts, mainly using ZQL.
 * @author shu
 */
public class SQLParser {
	
	public SQLParser(){
		//a sql parser...
	}
	
	// Helper function
	private ZQuery convertSqlToZQuery(String sql) throws ParseException {
		InputStream is = new ByteArrayInputStream(sql.getBytes());
		ZqlParser parser = new ZqlParser(is);
		ZQuery statement = (ZQuery) parser.readStatement();
		return statement;
	}
	
	/**
	 * Parse out different sections of a sql sentence.
	 * The result is stored in a K-V manner.
	 * Now supports: "columns", "tables".
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public Hashtable<String, List<String>> parseSQL(String sql) throws ParseException{
		Hashtable<String, List<String>> results = new Hashtable<String, List<String>>();
		ZQuery statement = convertSqlToZQuery(sql);
		//get tables;
		results.put("tables", parseTables(statement));
		//get columns;
		results.put("columns", parseColumns(statement));
		return results;
	}
	
	public List<String> parseTables(ZQuery statement) throws ParseException {
		LinkedList<String> tables =
				new LinkedList<String>();
		Vector<ZFromItem> ss = statement.getFrom();
		Iterator<ZFromItem> it = ss.iterator();
		while(it.hasNext()){
			ZFromItem item = it.next();
			tables.add(item.getTable());
		}
		return tables;
	}
	
	public List<String> parseColumns(ZQuery statement) throws ParseException{
		LinkedList<String> columns =
				new LinkedList<String>();
		Vector<ZSelectItem> ss = statement.getSelect();
		Iterator<ZSelectItem> it = ss.iterator();
		while(it.hasNext()){
			ZSelectItem item = it.next();
			columns.add(item.getColumn());
		}
		return columns;
	}
	
	public List<String> parseTables(String sql) throws ParseException {
		return parseTables(convertSqlToZQuery(sql));
	}
	
	public List<String> parseColumns(String sql) throws ParseException {
		return parseColumns(convertSqlToZQuery(sql));
	}
	
	public boolean hasWhere(String sql) throws ParseException {
		return (sql.contains("where") || sql.contains("WHERE"));
	}
	
	/**
	 * A tuple for storing the equal stuff in where.
	 * @author shu
	 */
	class WherePair {
		String attribute;
		Double value;
	}
	
	/**
	 * The ZExpression should be in format like < 'abc' = 100 >
	 * If the operator is not "=", then return null;
	 * @return
	 */
	private WherePair parseWhereTuple(ZExpression where) {
		if(where.getOperator().equals("=")){
			WherePair pair = new WherePair();
			pair.attribute = where.getOperand(0).toString();
			pair.value = Double.parseDouble(where.getOperand(1).toString());
			return pair;
		} else return null;
	}
	
	/**
	 * Only there are AND and "=" in where does this function work.
	 * If the situation not met, return a null;
	 * @param sql
	 * @return
	 * @throws ParseException 
	 */
	public HashMap<String, Double> parseWhere(String sql) throws ParseException {
		HashMap<String, Double> result = new HashMap<String, Double>();
		ZQuery query = convertSqlToZQuery(sql);
		ZExpression where = (ZExpression) query.getWhere();
		if(where.getOperator().equals("AND") || where.getOperator().equals("and")){
			Vector<ZExpression> operands = where.getOperands();
			for (ZExpression operand : operands) {
				WherePair wp = parseWhereTuple(operand);
				if(wp == null) return null;
				result.put(wp.attribute, wp.value);
			} 
		} else {
			WherePair wp = parseWhereTuple(where);
			if(wp == null) return null;
			result.put(wp.attribute, wp.value);
		}
		System.out.println(result);
		return result;
	}
	
	/**
	 * Testing
	 * More test casees could be found from
	 * http://zql.sourceforge.net/ztest.html
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		SQLParser parser = new SQLParser();
		//String sql = "SELECT FirstName, LastName, Address, City, State FROM EmployeeAddressTable;";
		String sql = "SELECT EMPLOYEEIDNO FROM EMPLOYEESTATISTICSTABLE WHERE POSITION = 10 and AGE = 15;";
		System.out.println(parser.parseSQL(sql));
		String sql2 = "SELECT EMPLOYEEIDNO FROM EMPLOYEESTATISTICSTABLE WHERE POSITION = 10;";
		//System.out.println(parser.hasWhere(sql));
		parser.parseWhere(sql2);
	}
}
