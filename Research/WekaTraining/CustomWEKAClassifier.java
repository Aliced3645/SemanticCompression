package WekaTraining;

import com.google.common.primitives.Ints;
import moa.classifiers.meta.WEKAClassifier;
import moa.core.InstancesHeader;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Package: semanticcompression
 * Class:
 * Description:
 */
public class CustomWEKAClassifier extends WEKAClassifier {

    static final Pattern treeDescriptionLine = Pattern.compile("^[\\s|]*\\b(\\w+)\\b");

    // removes any unnecessary data from the buffer for serialization
    public void trimSize() {
        this.instancesBuffer = new Instances(this.instancesBuffer, 0);
    }

    // returns the weka classifier used by this meta-classifier
    public Classifier getWEKAClassifier() {
        return this.classifier;
    }

    public int[] getDependencies(InstancesHeader dataHeader) {
        HashSet<Integer> columns = new HashSet<Integer>();
        HashMap<String, Integer> name_to_column = new HashMap<String, Integer>();
        for (int i = 1; i <= dataHeader.numAttributes(); i++) {
            String name = dataHeader.attribute(i - 1).name();
            name_to_column.put(name, i);
        }
        String description = getWEKAClassifier().toString();
        String[] lines = description.split("\n", -1);
        for (String line : lines) {
            if (!line.contains(":") || !(line.contains("<") || line.contains(">") || line.contains("=")))
                continue;
            Matcher m = treeDescriptionLine.matcher(line);
            if (!m.find())
                continue;
            String other_col = m.group(1);
            if (!name_to_column.containsKey(other_col))
                continue;
            columns.add(name_to_column.get(other_col));
        }
        return Ints.toArray(columns);
    }

    public static void printConnectionGraph(DecompressionStream inStream, File graphFile) {
        System.out.println("Writing graph to graph file...");
        // initialization
        moa.classifiers.Classifier[] classifiers = inStream.getClassifiers();
        HashMap<String, String> column_names = new HashMap<String, String>();
        for (int i = 1; i <= inStream.getHeader().numAttributes(); i++) {
            String name = inStream.getHeader().attribute(i - 1).name();
            column_names.put(name, String.format("col%d__%s", i, name));
        }
        StringBuilder sb = new StringBuilder();
        HashSet<String> found = new HashSet<String>();

        try {
            FileOutputStream fos = new FileOutputStream(graphFile);

            // print graph header
            fos.write("digraph model_graph {\n".getBytes());
            for (String column_name : column_names.values()) {
                if (found.contains(column_name))
                    continue;
                found.add(column_name);
                sb.append("\t");
                sb.append(column_name);
                sb.append(";\n");
            }
            fos.write(sb.toString().getBytes());

            // print graph
            for (int i = 1; i < classifiers.length; i++) {
                if (classifiers[i] != null) {
                    sb.setLength(0);
                    found.clear();
                    String this_col = inStream.getHeader().attribute(i - 1).name();
                    this_col = String.format("col%d__%s", i, this_col);

                    if (!(classifiers[i] instanceof CustomWEKAClassifier))
                        continue;
                    String description = ((CustomWEKAClassifier) classifiers[i]).getWEKAClassifier().toString();
                    //System.out.println(description);
                    String[] lines = description.split("\n", -1);
                    for (String line : lines) {
                        if (!line.contains(":") || !(line.contains("<") || line.contains(">") || line.contains("=")))
                            continue;
                        Matcher m = treeDescriptionLine.matcher(line);
                        if (!m.find())
                            continue;
                        String other_col = m.group(1);
                        if (found.contains(other_col) || !column_names.containsKey(other_col))
                            continue;
                        found.add(other_col);
                        sb.append("\t");
                        sb.append(this_col);
                        sb.append(" -> ");
                        sb.append(column_names.get(other_col));
                        sb.append(";\n");
                    }
                    fos.write(sb.toString().getBytes());
                }
            }

            // finish up
            fos.write("}".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
