package org.openimaj.data.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.dataset.ReadableListDataset;
import org.openimaj.io.ObjectReader;

/**
 * A ReadableListDataset that works on URLs.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <INSTANCE>
 */
public class URLListDataset<INSTANCE> extends ReadableListDataset<INSTANCE, InputStream> {

	private List<URL> urls;
	
	public URLListDataset(
			ObjectReader<INSTANCE, InputStream> reader) {
		super(reader);
		urls = new ArrayList<URL>();
	}
	
	public boolean addURL(URL url) {
		return urls.add(url);
	}

	@Override
	public INSTANCE getInstance(int index) {
		try {
			return reader.read(urls.get(index).openStream());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int numInstances() {
		return urls.size();
	}

	@Override
	public String getID(int index) {
		return urls.get(index).toString();
	}
}
