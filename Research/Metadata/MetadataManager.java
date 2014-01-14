package Metadata;

import weka.core.Instances;
import moa.streams.InstanceStream;
import WekaTraining.ColumnData;

public interface MetadataManager {

	/**
	 * We create a "compressed" table in the original database.
	 * We also store the models into the metadata database;
	 * @param trainingTable
	 * @param originalDb
	 * @param classified
	 * @param trainingInstances
	 * @param compressedColumns
	 * @param errorThreshold
	 */
	public void storeModels(String trainingTable, String originalDb, 
			int[] classified, Instances trainingInstances, ColumnData[] 
					compressedColumns, double errorThreshold) throws Exception;

}
