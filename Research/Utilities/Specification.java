package Utilities;

import moa.tasks.StandardTaskMonitor;

public class Specification {
	
	// the classify algorithm(classifiers) for compression and decompression
	public static final String ALGORITHM = "M5P";
	
	// the error for a misclassified nominal attribute
    public static final double MISCLASSIFICATION_ERROR = 1.0;
    
    // time limit for sampling
    public static final long SAMPLE_TIME_LIMIT_SECS = 60;
    
    // the max number of instances to use to train the model
    public static final int MAX_INSTANCES = 10000000;
    
    public static StandardTaskMonitor _taskMonitor = null;
}