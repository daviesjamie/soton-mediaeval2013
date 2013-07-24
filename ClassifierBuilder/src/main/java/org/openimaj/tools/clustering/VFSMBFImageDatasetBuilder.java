package org.openimaj.tools.clustering;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

/**
 * Builds a GroupedDataset from VFSListDatasets based on the given URLs.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class VFSMBFImageDatasetBuilder implements
		DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> {

	@Override
	public GroupedDataset<String, ListDataset<MBFImage>, MBFImage> build(
			String[] args, int maxSize) throws BuildException {
		
		final GroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataset =
			new MapBackedDataset<String, ListDataset<MBFImage>, MBFImage>();
		
		for (String path : args) {
			try {
				dataset.put(path, new VFSListDataset<MBFImage>(path, ImageUtilities.MBFIMAGE_READER));
			} catch (FileSystemException e) {
				throw new BuildException(e);
			}
		}
		
		return dataset;
	}

}
