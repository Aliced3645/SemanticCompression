package Offline;

import weka.core.Instances;
import weka.experiment.InstanceQuery;

/**
 * Interaction interface between WEKA and MySQL.
 * @author shu
 */
public class MySQLDBInterface implements DBInterface {

	private Instances getInstances(InstanceQuery iq, String tableName) throws Exception {
		iq.setQuery("select * from " + tableName + ";");
		Instances instances = iq.retrieveInstances();
		iq.disconnectFromDatabase();
		return instances;
	}
	 
	@Override
	public Instances retriveInstances(String tableName) throws Exception {
		InstanceQuery iq = new InstanceQuery();
		// the using the default jdbc url.
		iq.setDatabaseURL("jdbc:mysql://localhost/testdb?"
	              + "user=shu&password=shu");
		return getInstances(iq, tableName);
	}

	@Override
	public Instances retriveInstances(String tableName, String url)
			throws Exception {
		InstanceQuery iq = new InstanceQuery();
		iq.setDatabaseURL(url);
		return getInstances(iq, tableName);
	}

}
