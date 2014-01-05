package WekaTraining;

import moa.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.io.*;
import java.util.PriorityQueue;

/**
 * Package: semanticcompression
 * Class: Utilities
 * Description: Contains several static utility functions.
 */
public class Utilities {

    public static boolean useNumericRetrieval(Instances i) {
        return useNumericRetrieval(i, i.classIndex());
    }

    public static boolean useNumericRetrieval(Instances i, int classIndex) {
        return (i.attribute(classIndex).isNumeric() || i.attribute(classIndex).isDate());
    }

    public static boolean useNumericRetrieval(Instance i, int classIndex) {
        return (i.attribute(classIndex).isNumeric() || i.attribute(classIndex).isDate());
    }

    public static double numericValue(Classifier c, Instance i) {
        return c.getVotesForInstance(i)[0];
    }

    public static String nominalValue(Classifier c, Instance i) {
        return i.classAttribute().value(Utils.maxIndex(c.getVotesForInstance(i)));
    }

    public static boolean compressable(Classifier c, Instance i, int classIndex, double errorThreshold) {
        boolean numeric = useNumericRetrieval(i, classIndex - 1);
        return ((numeric && Utilities.percentError(c, i) <= errorThreshold)) || (!numeric && Utilities.correctlyClassified(c, i));
    }

    public static String stringValue(Instance i, int col) {
        if (useNumericRetrieval(i, col - 1))
            return Double.toString(i.value(col - 1));
        else
            return i.stringValue(col - 1);
    }

    public static double squareError(Classifier c, Instance i) {
        assert c.getVotesForInstance(i).length == 1;
        double realValue = i.classValue();
        double predValue = numericValue(c, i);
//        System.out.println(realValue + " -> " + predValue);
        double diff = realValue - predValue;
        return diff * diff;
    }

    public static double percentError(Classifier c, Instance i) {
        assert c.getVotesForInstance(i).length == 1;
        double realValue = i.classValue();
        double predValue = numericValue(c, i);
//        System.out.println(realValue + " -> " + predValue);
        double diff = realValue - predValue;
        return realValue == 0 ? diff : Math.abs(diff / realValue);
    }

    public static boolean correctlyClassified(Classifier c, Instance i) {
        String realValue = i.stringValue(i.classIndex());
        String predValue = nominalValue(c, i);
//        System.out.println(realValue + " -> " + predValue);
        return realValue.equals(predValue);
    }

    public static void writeColumnData(String trialsFile, PriorityQueue<ColumnData> columns) throws IOException {
        FileOutputStream fos = new FileOutputStream(trialsFile);
        ColumnData column;
        StringBuilder sb = new StringBuilder();
        while (!columns.isEmpty()) {
            column = columns.poll();
            sb.append(column._classIndex);
            sb.append("\t");
            sb.append(column._priority);
            sb.append("\t");
            sb.append(column._error);
            sb.append("\n");
            fos.write(sb.toString().getBytes());
            sb.setLength(0);
        }
    }

    public static PriorityQueue<ColumnData> readColumnData(String trialsFile) throws IOException {
        PriorityQueue<ColumnData> priorityQueue = new PriorityQueue<ColumnData>();
        BufferedReader br = new BufferedReader(new FileReader(new File(trialsFile)));
        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split("\t");
            int classIndex = Integer.parseInt(values[0]);
            int type = Integer.parseInt(values[1]);
            double error = Double.parseDouble(values[2]);
            if (error < 0)
                return null;
            priorityQueue.offer(new ColumnData(classIndex, error, -1, null, type));
        }
        return priorityQueue;
    }

    public static File getHeaderFile(File folder) {
        return new File(folder, folder.getName() + ".csv.header");
    }

    public static File getClassifiedFile(File folder) {
        return new File(folder, folder.getName() + ".csv.classified");
    }

    public static File getCompressedFile(File folder) {
        return new File(folder, folder.getName() + ".compressed.csv");
    }

    public static File getModelFile(File folder, int column) {
        return new File(folder, folder.getName() + ".model.moa." + Integer.toString(column));
    }
}
