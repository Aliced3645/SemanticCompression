package WekaTraining;

import moa.streams.ArffFileStream;
import moa.streams.InstanceStream;

import java.io.IOException;
import java.util.PriorityQueue;

/**
 * Package: semanticcompression
 * Class: IterativeCompression
 * Description: Iteratively compresses a dataset by developing classifiers to predict column values.
 */
public class IterativeCompression {

    // the minimum percent of compressed instances to compress an attribute
    private static final double COMPRESSION_THRESHOLD = 0.05;
    // the default error threshold used for compression
    private static final double DEFAULT_ERROR_THRESHOLD = 0.05;
    // minimum number of samples for training
    private static final int MIN_TRAINING_SAMPLES = 5000;
    // the ratio of the number of training instances to the size of the full dataset
    private static final double TRAINING_SAMPLES_RATIO = 0.10;
    // the max number of instances to sample from
    private static final int MAX_SAMPLE_POOL = 100000;

    private PriorityQueue<ColumnData> _columnData;
    private String _trainingFile, _testingFile, _outputFolder;
    private double _errorThreshold;

    public IterativeCompression(String trainingFile, String testingFile, PriorityQueue<ColumnData> columnData, String outputFolder, double errorThreshold) throws IOException {
        _trainingFile = trainingFile;
        _testingFile = testingFile;
        _columnData = columnData;
        _outputFolder = outputFolder;
        _errorThreshold = errorThreshold < 0 ? DEFAULT_ERROR_THRESHOLD : errorThreshold;
    }

    public void run() {
        int[] classified = new int[_columnData.size()];
        int classifiedSoFar = 0;
        CompressedOutputStream outputStream = null;
        try {
             outputStream = new CompressedOutputStream(_outputFolder);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        ColumnData[] compressedColumns = new ColumnData[_columnData.size()+1];

        while (_columnData.peek() != null) {
            System.out.println("----------");
            // get next column to attempt to compress
            ColumnData currColumn = _columnData.poll();
            System.out.println("Classifying " + currColumn);

            // primary columns should not be compressed
            if (currColumn._priority == ColumnData.PRIMARY) {
                System.out.println("Skipping primary column.");
                continue;
            }

            InstanceStream trainingStream = new ArffFileStream(_trainingFile, currColumn._classIndex);
            trainingStream = new ReservoirSampler(new StripClassifiedStream(new ArffFileStream(_trainingFile, currColumn._classIndex), classified), numSamples(trainingStream), MAX_SAMPLE_POOL);
            InstanceStream testingStream = new StripClassifiedStream(new ArffFileStream(_testingFile, currColumn._classIndex), classified);
            // this does most of the work
            ColumnData column = ClassifyColumn.classify(currColumn._classIndex, trainingStream, testingStream, _errorThreshold, outputStream);

            // if the classifier exists and is used at all, output the model and mark the column as having a classifier
            compressedColumns[column._classIndex] = column;
            if (column._classifier != null && column._percentCompressed > COMPRESSION_THRESHOLD) {
                classified[classifiedSoFar++] = column._classIndex;
            } else {
                column._classifier = null;
            }
        }

        System.out.println("Writing compressed output to folder '" + _outputFolder + "'...");
        try {
            outputStream.createCompressed(classified, new ArffFileStream(_trainingFile, 0), compressedColumns, _errorThreshold);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Creates a ranked list of ColumnData objects
    public static PriorityQueue<ColumnData> calculateColumnData(String trainingFile) {
        InstanceStream stream = new ArffFileStream(trainingFile, 0);
        int attributes = stream.getHeader().numAttributes();
        PriorityQueue<ColumnData> priorityQueue = new PriorityQueue<ColumnData>();
        for (int i = 1; i <= attributes; i++) {
            stream = new ReservoirSampler(new ArffFileStream(trainingFile, i), numSamples(stream), MAX_SAMPLE_POOL);
            //System.out.println("Classifying column " + i);
            priorityQueue.offer(ClassifyColumn.classify(i, stream, stream, -1, null));
        }
        return priorityQueue;
    }

    private static int numSamples(InstanceStream stream) {
        return (int) Math.max(MIN_TRAINING_SAMPLES, TRAINING_SAMPLES_RATIO * stream.estimatedRemainingInstances());
    }

}
