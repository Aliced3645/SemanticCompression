package Offline;

import weka.core.Instances;
import weka.experiment.InstanceQuery;

/**
 * Interaction interface between WEKA and MySQL.
 * @author shu
 */
public class MySQLDBInterface implements DBInterface {

	private String url;
	
	private Instances getInstances(InstanceQuery iq, String tableName) throws Exception {
		iq.setQuery("select * from " + tableName + ";");
		Instances instances = iq.retrieveInstances();
		instances.setClassIndex(instances.numAttributes() - 1);
		iq.disconnectFromDatabase();
		return instances;
	}
	 
	@Override
	public Instances retriveInstances(String tableName) throws Exception {
		InstanceQuery iq = new InstanceQuery();
		// the using the default jdbc url.
		if(this.url == null)
			iq.setDatabaseURL("jdbc:mysql://localhost/testdb?"
	              + "user=shu&password=shu");
		else 
			iq.setDatabaseURL(url);
		return getInstances(iq, tableName);
	}

	@Override
	public Instances retriveInstances(String tableName, String url)
			throws Exception {
		InstanceQuery iq = new InstanceQuery();
		iq.setDatabaseURL(url);
		return getInstances(iq, tableName);
	}

	@Override
	public void setURL(String url) {
		// TODO Auto-generated method stub
		this.url = url;
	}

	@Override
	public String getURL() {
		// TODO Auto-generated method stub
		return this.url;
	}

}
