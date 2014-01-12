package Offline;

import java.io.IOException;
import java.util.PriorityQueue;

import WekaTraining.ColumnData;
import WekaTraining.IterativeCompression;
import WekaTraining.Utilities;

/**
 * The purpose of this class is to load original database table (to be
 * compressed), transform into weka instances, and run classifier to get models
 * and column relationships, and store these into metadata store and database.
 * -Only for compression.
 * 
 * @author shu
 * 
 */
public class TrainingDriver {

	private static final String DEFAULT_SAMPLED_TRIALS_FILE = "sampledTrials.txt";

	private static void printUsageAndExit() {
		System.out
				.println("usage: compress <training table name> <testing table name> <output folder> <error threshold> <optional: sampled trials file>");
		System.out
				.println("note: a negative error threshold tells the program to use the default error threshold");
		System.out
				.println("note: if testing table is '-', then training file is used for testing as well");
		System.out
				.println("note: if sampled trials file is not provided, it will be generated and output to '"
						+ DEFAULT_SAMPLED_TRIALS_FILE + "'");
		System.exit(1);
	}

	/**
	 * This function is re-written from WekaTraining's main function We change
	 * the arff file to MySQL databse table name here.
	 * 
	 * @param args
	 *            argument list - (1) Name of table to be trained. (2) Name of
	 *            testing table. ( If the name is '-', then training table would
	 *            be the testing table. (3) Name of output FOLDER (to be changed
	 *            to database.) (4) Trails file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1
				|| !(args[0].equals("compress") || args[0].equals("decompress"))) {
			printUsageAndExit();
		}

		String trainingTable = args[1];
		String testingTable = args[2];
		String outputFolder = args[3]; // this might be changed.
		double errorThreshold = Double.parseDouble(args[4]);

		if (testingTable.equals("-"))
			testingTable = trainingTable;

		DBInterface dbInterface = new MySQLDBInterface();
		dbInterface.setURL("jdbc:mysql://localhost/testdb?"
				+ "user=shu&password=shu");

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
			columnData = IterativeCompression.calculateColumnDataFromTable(
					trainingTable, dbInterface);
			// write to sampled trails file..
			Utilities.writeColumnData(DEFAULT_SAMPLED_TRIALS_FILE,
					new PriorityQueue<ColumnData>(columnData));
		}

		System.out.println("Classifying and compressing the data:");
		/*
		 * IterativeCompression ic = new IterativeCompression( trainingTable,
		 * testingTable, columnData, outputFolder, errorThreshold); ic.run();
		 */
		IterativeCompression.runForTable(trainingTable, testingTable,
				columnData, outputFolder, errorThreshold, dbInterface);
	}
}
