package Offline;

import weka.core.Instances;

/**
 * This interface defines methods to load/store table/Weka instances from/to databases.
 * @author shu
 *
 */
public interface DBInterface {
	
	/**
	 * Retrive instances from database for learning.
	 * @param tableName
	 * @return WEKA instances.
	 * @throws Exception 
	 */
	public Instances retriveInstances(String tableName) throws Exception;
	
	/**
	 * Another alternative method for retrieving data by using additional JDBC URL.
	 */
	public Instances retriveInstances(String tableName, String url) throws Exception;
	
}
