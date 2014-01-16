package WekaTraining;

import moa.classifiers.Classifier;

/**
 * Package: semanticcompression
 * Class: ColumnData
 * Description: Used to store data about a column and its classification.
 */
public class ColumnData implements Comparable<ColumnData> {

    public final int _classIndex;
    final double _error; // used for numeric attributes
    final double _percentCompressed;
    public Classifier _classifier;
    final int _priority;

    final static int PRIMARY = -1;
    final static int SECONDARY = -2;
    final static int TERTIARY = -3;

    ColumnData(int classIndex, double error, double percentCompressed, Classifier classifier) {
        if (error < 0 && percentCompressed < 0)
            throw new IllegalArgumentException("Both error and percent compressed cannot be less than zero");

        _classIndex = classIndex;
        _error = error;
        _percentCompressed = percentCompressed;
        _classifier = classifier;
        _priority = TERTIARY;
    }

    ColumnData(int classIndex, double error, double percentCompressed, Classifier classifier, int type) {
        if (error < 0 && percentCompressed < 0)
            throw new IllegalArgumentException("Both error and percent compressed cannot be less than zero");

        _classIndex = classIndex;
        _error = error;
        _percentCompressed = percentCompressed;
        _classifier = classifier;
        _priority = type <= PRIMARY && type >= TERTIARY ? type : TERTIARY;
    }

    // Need to manipulate ordering for PriorityQueue so highest accuracy/lowest error is at head
    public int compareTo(ColumnData c) {
        int ret = compareInts(this._priority, c._priority);
        return ret == 0 ? Double.compare(this._error, c._error) : ret;
    }

    // copied from 1.7 implementation
    public static int compareInts(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public String toString() {
        return "[ColumnData: (Column: " + Integer.toString(_classIndex) + ", Type: " + Integer.toString(_priority) + (_error >= 0 ? ", Error: " + Double.toString(_error) : ", Percent Compressed: " + Double.toString(_percentCompressed)) + ")]";
    }
}
