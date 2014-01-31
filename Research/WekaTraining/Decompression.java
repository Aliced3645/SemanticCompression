package WekaTraining;

import weka.core.converters.AbstractSaver;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.IOException;

/**
 * Package: semanticcompression
 * Class: Decompression
 * Description: Used to decompress data that has been compressed.
 */
public class Decompression {

    DecompressionStream _inStream;
    File _outputFile;
    File _graphFile;

    public Decompression(String inputFolder, String outputFile, String graphFile) {
    	//Inside the constructor.... there is a bunch of stuff..
        _inStream = new DecompressionStream(inputFolder);
        _outputFile = new File(outputFile);
        _graphFile = new File(graphFile);
    }

    public void run() {
        ArffSaver saver = new ArffSaver();
        try {
            saver.setFile(_outputFile);
            saver.setRetrieval(AbstractSaver.INCREMENTAL);
            saver.setStructure(_inStream.getHeader());
            System.out.println("Writing output to output file...");
            // the DecompressionStream does all the work
            // hasMoreInstances()
            // Call repeatedly!!
            while (_inStream.hasMoreInstances()) {
                saver.writeIncremental(_inStream.nextInstance());
            }
            saver.writeIncremental(null);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //Watch out!!
        _inStream.restart();
        CustomWEKAClassifier.printConnectionGraph(_inStream, _graphFile);
    }
}
