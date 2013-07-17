package org.openimaj.tools.clustering;

import java.io.IOException;
import java.util.Iterator;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101.Record;

/**
 * Builds a version of Caltech101 with(out) the specified categories.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Caltech101DatasetBuilder implements
		DatasetBuilder<ListDataset<MBFImage>> {

	@Override
	/**
	 * @param args  an optional mode specifier (mode=exclude), followed by 
	 * 				the names of categories to include in the dataset
	 */
	public Dataset<MBFImage> build(String[] args) throws BuildException {
		VFSGroupDataset<Record<MBFImage>> ct101;
		
		try {
			 ct101 = Caltech101.getData(ImageUtilities.MBFIMAGE_READER);
		} catch (IOException e) {
			throw new BuildException(e);
		}
		
		if (args.length == 0) {
			throw new BuildException("No arguments");
		}
		
		// Check for a mode specifier at position 0.
		String[] modeSpec = args[0].split("=");
		
		int start = 0;
		boolean exclude = false;
		
		if (modeSpec[0].equals("mode")) {
			start++;
			
			if (modeSpec.length == 2 && modeSpec[1].equals("exclude")) {
				exclude = true;
			}
		}
		
		final MapBackedDataset<String, ListDataset<Record<MBFImage>>, Record<MBFImage>> intermediary = 
			new MapBackedDataset<String, ListDataset<Record<MBFImage>>, Record<MBFImage>>();
		
		// If we're excluding, add all the categories to the intermediary 
		// dataset to allow removal.
		if (exclude) {
			for (String key : ct101.getGroups()) {
				intermediary.add(key, ct101.get(key));
			}
		}
		
		for (int i = start; i < args.length; i++) {
			CaseInsensitiveString key = new CaseInsensitiveString(args[i]);
			
			if (ct101.containsKey(key)) {
				if (!exclude) {
					intermediary.add(key.toString(), ct101.get(key));
				} else {
					intermediary.remove(key);
				}
			} else {
				throw new BuildException("Invalid category: " + key);
			}
		}
		
		// Create a new Dataset to unwrap the MBFImages from the Records.
		return new Dataset<MBFImage>() {

			@Override
			public Iterator<MBFImage> iterator() {
				
				// Unwrapping requires a custom Iterator.
				return new Iterator<MBFImage>() {
					Iterator<Record<MBFImage>> iter = intermediary.iterator();

					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public MBFImage next() {
						return iter.next().getImage();
					}

					@Override
					public void remove() {
						iter.remove();
					}
					
				};
			}

			@Override
			public MBFImage getRandomInstance() {
				return intermediary.getRandomInstance().getImage();
			}

			@Override
			public int numInstances() {
				return intermediary.numInstances();
			}
			
		};
	}

}
