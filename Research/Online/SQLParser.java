package Online;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
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
	
	Connection conn;
	
	public SQLParser(){
		//a sql parser...
		
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
		InputStream is = new ByteArrayInputStream(sql.getBytes());
		ZqlParser parser = new ZqlParser(is);
		ZQuery statement = (ZQuery) parser.readStatement();
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
	
	/**
	 * Testing
	 * More test casees could be found from
	 * http://zql.sourceforge.net/ztest.html
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		SQLParser parser = new SQLParser();
		String sql = "SELECT FirstName, LastName, Address, City, State FROM EmployeeAddressTable;";
		System.out.println(parser.parseSQL(sql));
	}
}
