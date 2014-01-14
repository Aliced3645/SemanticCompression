package MySQL;

import java.util.Enumeration;

import moa.streams.CachedInstancesStream;
import moa.streams.InstanceStream;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.experiment.InstanceQuery;

/**
 * Related Knowledge from: http://weka.wikispaces.com/Databases
 * http://weka.wikispaces.com/Use+Weka+in+your+Java+code This class is for
 * testing using Weka working with MySqL.
 * 
 * @author shu
 */
public class WekaIntegrationTest {

	/**
	 * Retrieving instances from MySQL database.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		try {
			InstanceQuery iq = new InstanceQuery();
			iq.setDatabaseURL("jdbc:mysql://localhost/testdb?"
		              + "user=shu&password=shu");
			String query = Utils.getOption('Q', args);
			if (query.length() == 0) {
				iq.setQuery("select * from table2");
			} else {
				iq.setQuery(query);
			}
			iq.setOptions(args);
			try {
				Utils.checkForRemainingOptions(args);
			} catch (Exception e) {
				System.err
						.println("Options for weka.experiment.InstanceQuery:\n");
				Enumeration en = iq.listOptions();
				while (en.hasMoreElements()) {
					Option o = (Option) en.nextElement();
					System.err.println(o.synopsis() + "\n" + o.description());
				}
				System.exit(1);
			}

			Instances instances = iq.retrieveInstances();
			iq.disconnectFromDatabase();
			// query returned no result -> exit
			if (instances == null)
				return;
			// The dataset may be large, so to make things easier we'll
			// output an instance at a time (rather than having to convert
			// the entire dataset to one large string)
			
//			System.out.println(new Instances(instances, 0));
			System.out.println(instances.attribute(0).name());

			for (int i = 0; i < instances.numInstances(); i++) {
				//System.out.println(instances.instance(i));
			}
			
			/*
	    	InstanceStream stream = 
	    			new CachedInstancesStream (instances);
	    	int attributes = stream.getHeader().numAttributes();
	    	System.out.println(attributes);
			*/
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}

	}

}
