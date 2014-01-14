package Metadata;

import moa.streams.InstanceStream;
import WekaTraining.ColumnData;

public interface MetadataManager {

	/**
	 * We create a "compressed" table in the original database.
	 * We also store the models into the metadata database;
	 * @param trainingTable
	 * @param originalDb
	 * @param classified
	 * @param inputStream
	 * @param compressedColumns
	 * @param errorThreshold
	 */
	public void storeModels(String trainingTable, String originalDb, 
			int[] classified, InstanceStream inputStream, ColumnData[] 
					compressedColumns, double errorThreshold);

}
