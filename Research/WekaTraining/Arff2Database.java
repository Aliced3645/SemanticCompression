package WekaTraining;

import weka.core.*;
import weka.core.converters.*;
import java.io.*;

  public class Arff2Database {
 
    /**
     * loads a dataset into a MySQL database
     *
     * @param args    the commandline arguments
     */
    public static void main(String[] args) throws Exception {
      Instances data = new Instances(new BufferedReader(new FileReader(args[0])));
      data.setClassIndex(data.numAttributes() - 1);
 
      DatabaseSaver save = new DatabaseSaver();
      save.setUrl("jdbc:mysql://localhost/testdb?"
				+ "user=shu&password=shu");

      save.setInstances(data);
      save.setRelationForTableName(false);
      save.setTableName(args[0]);
      save.connectToDatabase();
      System.out.println("Begin transfering " + args[0] + " into mysql database...");
      save.writeBatch();
      System.out.println("Finished!");
    }
  }