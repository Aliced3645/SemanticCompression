package Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import com.google.common.io.Files;

/**
 * A helper class supporting two types of column (in arff file format) reading.
 * 1. Direct copy to destination
 * 2. 
 * @author shu
 *
 */
public class ColumnReader {
	
	//It is used to simulate the in-memory operation.
	public static void copyDirectly(String tableName, String columnName,
			String outputDir) throws IOException{
		String path = tableName + "/" + columnName + ".arff";
		String outputPath = outputDir + "/" + columnName + ".arff";
		File from = new File(path);
		File to = new File(outputPath);
		Files.copy(from, to);
	}
	
	//It is used to simulate regualar load of column data.
	public static void readAndWrite(String tableName, String columnName, String outputDir) throws IOException{
		
		String path = tableName + "/" + columnName + ".arff";
		String outputPath = outputDir + "/" + columnName + ".arff";
		BufferedReader reader = new BufferedReader(new FileReader(
				path));
		Instances instances = new Instances(reader);
		reader.close();
		
		Attribute attribute = instances.attribute(0);
		ArrayList<Attribute> attrinfo = new ArrayList<Attribute>();
		attrinfo.add(attribute);
		Instances destInstances = new Instances(attribute.name(), attrinfo, instances.size());
		
		for(int i = 0; i < instances.size(); i ++) {
			Instance instance = instances.get(i);
			Instance instanceToAdd = new DenseInstance(1);
			instanceToAdd.setValue(0, instance.value(0));
			destInstances.add(instance);
		}
		
		ArffSaver saver = new ArffSaver();
		saver.setInstances(destInstances);
		saver.setFile(new File(outputPath));
		saver.setDestination(new File(outputPath));
		saver.writeBatch();
	
	}
	
}
