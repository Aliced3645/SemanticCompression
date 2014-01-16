package WekaTraining;

import moa.core.InstancesHeader;
import moa.core.SerializeUtils;
import moa.streams.InstanceStream;
import weka.core.Instance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Package: semanticcompression
 * Class: CompressedOutputStream
 * Description: Used to output classified data to csv/model.moa files.
 */
public class CompressedOutputStream {

    private File _outputFolder;

    public CompressedOutputStream(String outputFolderPath) throws IOException {
        if (!recursivelyDelete(outputFolderPath))
            throw new IllegalArgumentException("Could not delete existing contents of output folder");
        _outputFolder = new File(outputFolderPath);
        if (!_outputFolder.mkdir() && !_outputFolder.exists() && !_outputFolder.isDirectory())
            throw new IllegalArgumentException("Output folder could not be created");
    }

    // writes the compressed csv file
    public void createCompressed(int[] classified, InstanceStream inputStream, ColumnData[] compressedColumns, double errorThreshold) throws IOException {
        SerializeUtils.writeToFile(Utilities.getHeaderFile(_outputFolder), inputStream.getHeader());
        SerializeUtils.writeToFile(Utilities.getClassifiedFile(_outputFolder), classified);
        FileOutputStream fos = new FileOutputStream(Utilities.getCompressedFile(_outputFolder));

        for (int i = 1; i <= classified.length; i++) {
            ColumnData c = compressedColumns[i];
            SerializeUtils.writeToFile(Utilities.getModelFile(_outputFolder, c._classIndex), c._classifier);
        }

        InstancesHeader header = inputStream.getHeader();
        StringBuilder sb = new StringBuilder();
        
        //by rows..
        while (inputStream.hasMoreInstances()) {
            Instance inst = inputStream.nextInstance();
            sb.setLength(0);
            //by columns
            for (int i = 1; i < compressedColumns.length; i++) {
                ColumnData c = compressedColumns[i];
                header.setClassIndex(i-1);
                inst.setDataset(header);
                if (c._classifier == null || !Utilities.compressable(c._classifier, inst, i, errorThreshold))
                    sb.append(Utilities.stringValue(inst, c._classIndex));
                sb.append(",");
            }
            // replace the last comma with a newline
            sb.replace(sb.length() - 1, sb.length(), "\n");
            fos.write(sb.toString().getBytes());
        }
        fos.close();
    }
    

    private boolean recursivelyDelete(String path) {
        File f = new File(path);
        if (!f.exists())
            return true;
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                if (!recursivelyDelete(c.getPath()))
                    return false;
            }
            return true;
        } else {
            return f.delete();
        }
    }
    
}
