package WekaTraining;

import moa.AbstractMOAObject;
import moa.classifiers.Classifier;
import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import moa.streams.InstanceStream;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.io.*;

/**
 * Package: semanticcompression
 * Class: DecompressionStream
 * Description: Implements InstanceStream and reads compressed data.
 */
public class DecompressionStream extends AbstractMOAObject implements InstanceStream {

    private InstancesHeader _header;
    private int[] _classified;
    private File _inputFolder;
    private BufferedReader _inputStream;
    private Classifier[] _classifiers;
    private Instance _nextInstance;
    private long _linesLeftInFile;

    public DecompressionStream(String inputFolder) {
        _inputFolder = new File(inputFolder);
        clearFields();
        //Call restart() in constructor!
        //Translate folder into header, input stream, etc..
        restart();
        if (_header == null || _classified == null || _inputStream == null || _classifiers == null)
            throw new IllegalArgumentException("Could not read input");
    }
    
    public DecompressionStream(String tableName, String columnName){
    	
    }

    public InstancesHeader getHeader() {
        return _header;
    }

    public long estimatedRemainingInstances() {
        return _linesLeftInFile;
    }

    public boolean hasMoreInstances() {
        return _nextInstance != null;
    }

    public Instance nextInstance() {
    	//start from an empty instance.
        Instance oldInstance = _nextInstance;
        _nextInstance = new DenseInstance(_header.numAttributes());
        _nextInstance.setDataset(_header);
        _linesLeftInFile--;
        String line;
        double val;

        // retrieve stored attributes
        try {
            line = _inputStream.readLine();
            if (line == null || line.equals("")) {
                // something went wrong
                _nextInstance = null;
            } else {
            	//recover instnaces.
                String[] cols = line.split(",", -1);
                if (cols.length != _nextInstance.numAttributes()) {
                    _nextInstance = null;
                } else {
                    for (int i = 0; i < cols.length; i++) {
                        if (cols[i].equals("")) {
                            _nextInstance.setMissing(i);
                        } else if (_header.attribute(i).isNumeric()) {
                            // numeric attribute
                            try {
                                val = Double.parseDouble(cols[i]);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                _nextInstance = null;
                                break;
                            }
                            _nextInstance.setValue(i, val);
                        } else {
                            // nominal, string, and other attributes
                            _nextInstance.setValue(i, cols[i]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            _nextInstance = null;
        }

        // !!!!predict compressed attributes
        for (int i = 0; _nextInstance != null && i < _classified.length && _classified[i] != 0; i++) {
            int col = _classified[i];
            if (_nextInstance.isMissing(col-1)) {
                _header.setClassIndex(col-1);
                _nextInstance.setDataset(_header);
                if (Utilities.useNumericRetrieval(_header, col-1))
                    _nextInstance.setValue(col-1, Utilities.numericValue(_classifiers[col], _nextInstance));
                else
                    _nextInstance.setValue(col-1, Utilities.nominalValue(_classifiers[col], _nextInstance));
            }
        }

        return oldInstance;
    }

    public boolean isRestartable() {
        return true;
    }

    public void restart() {
        try {
            clearFields();
            _header = (InstancesHeader) SerializeUtils.readFromFile(Utilities.getHeaderFile(_inputFolder));
            _classified = (int[]) SerializeUtils.readFromFile(Utilities.getClassifiedFile(_inputFolder));
            _inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(Utilities.getCompressedFile(_inputFolder))));
            _classifiers = new Classifier[_header.numAttributes()+1];

            // store all the classifiers and fileinputstreams
            for (int i = 1; i <= _header.numAttributes(); i++) {
                try {
                    _classifiers[i] = (Classifier) SerializeUtils.readFromFile(Utilities.getModelFile(_inputFolder, i));
                    //System.out.println("=========================");
                    //System.out.println(_header.attribute(i - 1).name());
                    //System.out.println(_classifiers[i]);
                } catch (IOException e) {
                    _classifiers[i] = null;
                }
            }

            _nextInstance = null;
            nextInstance();
        } catch (ClassNotFoundException e) {
            clearFields();
            e.printStackTrace();
        } catch (IOException e) {
            clearFields();
            e.printStackTrace();
        }
    }

    public void getDescription(StringBuilder sb, int indent) {}

    public Classifier[] getClassifiers() {
        return _classifiers;
    }

    private void clearFields() {
        _header = null;
        _classified = null;
        _inputStream = null;
        _nextInstance = null;
        _classifiers = null;
        _linesLeftInFile = -1;
    }
}
