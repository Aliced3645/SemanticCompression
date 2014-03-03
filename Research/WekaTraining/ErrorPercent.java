package WekaTraining;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import weka.core.Instances;

public class ErrorPercent {
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

		
		File file = new File(columnName + ".ErrorPercent");
		FileOutputStream fos = new FileOutputStream(file);
		
		for(int i = 0; i < rowNum; i++) {
			double srcValue = instancesSrc.get(i).value(0);
			double predValue = instancesPred.get(i).value(0);
			double diff = srcValue - predValue;
			
			double result = srcValue == 0 ? Math.abs(diff) : Math.abs(diff / srcValue);
			
			fos.write(String.valueOf((i+1) + ": " + result + "\n").getBytes());
			
		}
		
		fos.close();
		
		System.out.println("Output Error Percent by row finished. File stored as " + file.getName());

	}
}
