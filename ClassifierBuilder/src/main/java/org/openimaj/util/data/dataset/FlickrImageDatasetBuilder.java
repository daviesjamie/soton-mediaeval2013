package org.openimaj.util.data.dataset;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.dataset.FlickrImageDataset;
import org.openimaj.util.api.auth.common.FlickrAPIToken;

public class FlickrImageDatasetBuilder implements DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> {

	@Override
	/**
	 * Args should be of the form apikey=<API key>,secret=<API secret>,<search terms>...
	 */
	public GroupedDataset<String, ListDataset<MBFImage>, MBFImage> build(
			String[] args, int maxSize) throws BuildException {
		if (args.length < 2) {
			throw new BuildException("Not enough arguments.");
		}
		
		List<String> searchTerms = new ArrayList<String>();
		String apiKey = null;
		String secret = null;
		
		// Gather args.
		for (String arg : args) {
			String[] split = arg.split("=", 2);
			
			if (split.length == 2 && split[0].equalsIgnoreCase("apikey")) {
				apiKey = split[1];
			} else if (split.length == 2 && split[0].equalsIgnoreCase("secret")) {
				secret = split[1];
			} else {
				searchTerms.add(split[0]);
			}
		}
		
		if (apiKey == null || secret == null) {
			throw new BuildException("Not all API key information was given.");
		}
		
		FlickrAPIToken apiToken = new FlickrAPIToken(apiKey, secret);
		
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> allDatasets = 
			new MapBackedDataset<String, ListDataset<MBFImage>, MBFImage>();
		
		for (String searchTerm : searchTerms) {
			FlickrImageDataset<MBFImage> dataset;
			
			try {
				dataset = FlickrImageDataset.create(ImageUtilities.MBFIMAGE_READER, apiToken, searchTerm, maxSize);
			} catch (Exception e) {
				throw new BuildException(e);
			}
			
			System.out.println("Adding dataset for search term: " + searchTerm);
			allDatasets.put(searchTerm, dataset);
		}
		
		return allDatasets;
	}

}
