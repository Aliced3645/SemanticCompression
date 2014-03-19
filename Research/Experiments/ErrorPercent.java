package Experiments;

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
		String outputFolder = args[3];
		
		BufferedReader readerSrc = new BufferedReader(
                new FileReader(columnsFileFolder + "/" + columnName + ".arff"));
		
		BufferedReader readerPred = new BufferedReader(
                new FileReader(predictFilesFolder + "/" + columnName + ".arff"));
		
		Instances instancesSrc = new Instances(readerSrc);
		Instances instancesPred = new Instances(readerPred);

		readerSrc.close();
		readerPred.close();
		
		int rowNum = instancesSrc.size();

		File folder = new File(outputFolder);
		folder.mkdir();
		
		File file = new File(outputFolder + "/" + columnName + ".ErrorPercent");
		
		FileOutputStream fos = new FileOutputStream(file);
		
		int e_1 = 0;
		int e_5 = 0;
		int e_10 = 0;
		int e_50 = 0;
		int e_100 = 0;
		int e_inf = 0;
		
		for(int i = 0; i < rowNum; i++) {
			double srcValue = instancesSrc.get(i).value(0);
			double predValue = instancesPred.get(i).value(0);
			double diff = srcValue - predValue;
			
			double result = srcValue == 0 ? Math.abs(diff) : Math.abs(diff / srcValue);
			
			
			if(result < 0.01) {
				e_1++;
				
			}
			
			else if(result >= 0.01 && result < 0.05) {
				e_5++;
			}
			
			else if(result >= 0.05 && result < 0.1) {
				e_10++;				
			}
			
			else if(result >= 0.1 && result < 0.5) {
				e_50++;
			}
			
			else if(result >= 0.5 && result < 1) {
				e_100++;
			}
			
			else {
				e_inf++;
			}
			
			fos.write(String.valueOf((i+1) + ": " + result + "\n").getBytes());
			
		}
		
		fos.write(String.valueOf("\n").getBytes());
		fos.write(String.valueOf("---------------------------------------------" + "\n").getBytes());
		fos.write(String.valueOf("Statistics:" + "\n").getBytes());
		fos.write(String.valueOf("\n").getBytes());
		
		double e_1_p = (double) e_1 / rowNum;
		double e_5_p = (double) e_5 / rowNum;
		double e_10_p = (double) e_10 / rowNum;
		double e_50_p = (double) e_50 / rowNum;
		double e_100_p = (double) e_100 / rowNum;
		double e_inf_p = (double) e_inf / rowNum;
		
		fos.write(String.valueOf(e_1_p + " [0% - 1%]" + "\n").getBytes());
		fos.write(String.valueOf(e_5_p + " [1% - 5%]" + "\n").getBytes());
		fos.write(String.valueOf(e_10_p + " [5% - 10%]" + "\n").getBytes());
		fos.write(String.valueOf(e_50_p + " [10% - 50%]" + "\n").getBytes());
		fos.write(String.valueOf(e_100_p + " [50% - 100%]" + "\n").getBytes());
		fos.write(String.valueOf(e_inf_p + " [100% - infinity]" + "\n").getBytes());
		
		fos.close();
		
		System.out.println("Output Error Percent by row finished. File stored as " + file.getName());
		
		System.out.println(e_1_p + " [0% - 1%]");
		System.out.println(e_5_p + " [1% - 5%]");
		System.out.println(e_10_p + " [5% - 10%]");
		System.out.println(e_50_p + " [10% - 50%]");
		System.out.println(e_100_p + " [50% - 100%]");
		System.out.println(e_inf_p + " [100% - infinity]");

	}
}
