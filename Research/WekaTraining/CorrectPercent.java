package WekaTraining;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


import weka.core.Instances;

public class CorrectPercent {
	//Numeric only for now.
	
	public static void main(String[] args) throws IOException {
		String columnsFileFolder = args[0];
		String predictFilesFolder = args[1];
		String columnName = args[2];
		
		BufferedReader readerSrc = new BufferedReader(
                new FileReader(columnsFileFolder + "/" + columnName + ".arff"));
		
		BufferedReader readerPred = new BufferedReader(
                new FileReader(predictFilesFolder + "/" + columnName + ".arff"));
		
		Instances instancesSrc = new Instances(readerSrc);
		Instances instancesPred = new Instances(readerPred);

		readerSrc.close();
		readerPred.close();
		
		int rowNum = instancesSrc.size();
		double errorSum = 0.0;
		
		for(int i = 0; i < rowNum; i++) {
			errorSum += Math.abs((instancesSrc.get(i).value(0) - instancesPred.get(i).value(0)) / instancesSrc.get(i).value(0));
		}
		
		double correctPercent = 1 - errorSum / (double)rowNum;
		
		System.out.println(correctPercent);
	}
}
