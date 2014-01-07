package WekaTraining;

import java.io.IOException;
import java.util.PriorityQueue;

import weka.core.Instance;

/**
 * Package: semanticcompression
 * Class: Main
 * Description: CLI interface for the program.
 */
public class Main {

//    compress sqlPhotoObjAll-2-5061-137_0015.arff - classified -1
//    decompress classified output.arff
//    compress covtype.arff - classified .05 sampledTrials.txt
//    decompress classified output.arff

    private static final String DEFAULT_SAMPLED_TRIALS_FILE = "sampledTrials.txt";

    private static void printUsageAndExit() {
        System.out.println("usage: compress <training .arff file> <testing .arff file> <output folder> <error threshold> <optional: sampled trials file>");
        System.out.println("note: a negative error threshold tells the program to use the default error threshold");
        System.out.println("usage: decompress <input folder> <output .arff file> <output .dot graph file>");
        System.out.println("note: if testing file is '-', then training file is used for testing as well");
        System.out.println("note: if sampled trials file is not provided, it will be generated and output to '" + DEFAULT_SAMPLED_TRIALS_FILE + "'");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || !(args[0].equals("compress") || args[0].equals("decompress"))) {
            printUsageAndExit();
            Instance i;
        }
        if (args[0].equals("compress")) {
            if (args.length != 5 && args.length != 6)
                printUsageAndExit();

            String trainingFile = args[1];
            String testingFile = args[2];
            String outputFolder = args[3];
            double errorThreshold = Double.parseDouble(args[4]);
            if (testingFile.equals("-"))
                testingFile = trainingFile;
            PriorityQueue<ColumnData> columnData;
            if (args.length == 6) {
                System.out.println("Reading sampled trials file:");
                columnData = Utilities.readColumnData(args[5]);
                if (columnData == null) {
                    System.out.println("Error when reading sampled trials file");
                    System.exit(1);
                }
            } else {
                System.out.println("Creating sampled trials file:");
                columnData = IterativeCompression.calculateColumnData(trainingFile);
                Utilities.writeColumnData(DEFAULT_SAMPLED_TRIALS_FILE, new PriorityQueue<ColumnData>(columnData));
            }
            System.out.println("Classifying and compressing the data:");
            IterativeCompression ic = new IterativeCompression(trainingFile, testingFile, columnData, outputFolder, errorThreshold);
            ic.run();
        } else if (args[0].equals("decompress")) {
            if (args.length != 4)
                printUsageAndExit();

            String inputFolder = args[1];
            String outputFile = args[2];
            String graphFile = args[3];
            Decompression dc = new Decompression(inputFolder, outputFile, graphFile);
            dc.run();
        }
    }
}
