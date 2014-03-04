package Offline;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;



/**
 * Splits columns out of an arff file.
 * Each column becomes a single arff file.
 * @author shu
 *
 */
public class ArffColumnSplitter {
	
	/**
	 * 1st arg: the file to be splitted
	 * 2st arg: the folder to put the splitted arffs.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{
		if(args.length != 2){
			System.out.println("Missing arguments.");
			System.exit(0);
		}
		
		String from = args[0];
		String folder = args[1];
		BufferedReader reader = new BufferedReader(
                new FileReader(from));
		Instances instances = new Instances(reader);
		reader.close();
		int numColumns = instances.numAttributes();
		//an array of instances for columns.
		ArrayList<Instances> columns = new ArrayList<Instances>();
		//initialize
		for(int i = 0; i < numColumns; i ++){
			Attribute attribute = instances.attribute(i);
			ArrayList<Attribute> attrinfo = new ArrayList<Attribute>();
			attrinfo.add(attribute);
			Instances columnInstances = new Instances(attribute.name(), attrinfo, instances.size());
			columns.add(columns.size(), columnInstances);
		}
		
		//Store and split the values;
		for(int i = 0; i < instances.size(); i ++){
			Instance instance = instances.get(i);
			for(int j = 0; j < numColumns; j ++){
				Instance instanceToAdd = new DenseInstance(1);
				instanceToAdd.setValue(0, instance.value(j));
				columns.get(j).add(instanceToAdd);
			}
		}
		
		//Finally stores in files.
		for(Instances columnInstances : columns) {
			StringBuilder sb = new StringBuilder(folder);
			sb.append('/');
			sb.append(columnInstances.attribute(0).name());
			sb.append(".arff");
			ArffSaver saver = new ArffSaver();
			saver.setInstances(columnInstances);
			saver.setFile(new File(sb.toString()));
			saver.setDestination(new File(sb.toString()));
			saver.writeBatch();
		}
		
	}
}
