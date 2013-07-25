package org.openimaj.tools.clustering;

import java.io.IOException;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.image.MBFImage;

public abstract class DatasetBuilderUtilities {
	/**
	 * sourceString should be a semicolon (;) delimited list of one or more 
	 * dataset specifiers, where each specifier is of the form <type>:<args>, 
	 * where <type> defines the DatasetBuilder to pass <args> to, and where 
	 * <args> is a comma (,) delimited list of arguments, which may be a single
	 * value or a <key>=<value> pair. For example:
	 * 
	 * Caltech101:mode=exclude,flamingo,faces
	 * 
	 * is a valid sourceString.
	 * 
	 * @throws BuildException 
	 */
	public static GroupedDataset<String, ListDataset<MBFImage>, MBFImage> buildMBFImageDataset(String sourceString, int maxSize) throws IOException, InvalidDatasetException, BuildException {
		System.out.println("Aggregating datasets...");
		
		// Datasets are semicolon-separated.
		String[] subSets = sourceString.split(";");
		
		AggregateGroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataset =
			new AggregateGroupedDataset<String, ListDataset<MBFImage>, MBFImage>();

		for (String subSet : subSets) {
			String[] components = subSet.split(":", 2);
			
			DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> builder = getDatasetBuilder(components[0]);
			
			if (builder == null) {
				throw new InvalidDatasetException(components[0]);
			}
			
			System.out.print("Adding dataset: " + components[0]);
			
			String[] args = null;
			if (components.length == 2) {
				System.out.println(" with arguments: " + components[1]);
				
				args = components[1].split(",");
			} else {
				System.out.println();
			}
			
			GroupedDataset<String, ListDataset<MBFImage>, MBFImage> subDataset = 
					builder.build(args, maxSize);
			
			dataset.addDataset(subDataset);
		}
		
		GroupedDataset<String, AggregateListDataset<MBFImage>, MBFImage> cast1 = dataset;
		GroupedDataset<String, ? extends ListDataset<MBFImage>, MBFImage> cast2 = cast1;
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> cast3 = (GroupedDataset<String, ListDataset<MBFImage>, MBFImage>) cast2;
		
		return cast3;
	}

	private static DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> getDatasetBuilder(
			String dataset) throws BuildException {
		switch (dataset) {
		case "Caltech101": 		return new Caltech101DatasetBuilder();
		case "Flickr": 			return new FlickrImageDatasetBuilder();
		case "GoogleImages": 	return new GoogleImagesDatasetBuilder();
		case "VFS": 			return new VFSMBFImageDatasetBuilder();
		default:		throw new BuildException("Unrecognised dataset builder: " + dataset);
		}
	}
}
