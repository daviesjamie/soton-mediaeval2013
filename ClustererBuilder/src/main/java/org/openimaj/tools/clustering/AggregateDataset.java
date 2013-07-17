package org.openimaj.tools.clustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openimaj.data.dataset.Dataset;

import com.google.common.collect.Iterators;

/**
 * Aggregate dataset that merges its component datasets back-to-back.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <INSTANCE>
 */
public class AggregateDataset<INSTANCE> implements Dataset<INSTANCE> {
	List<Dataset<INSTANCE>> datasets = new ArrayList<Dataset<INSTANCE>>();
	
	public boolean add(Dataset<INSTANCE> dataset) {
		return datasets.add(dataset);
	}

	@Override
	public Iterator<INSTANCE> iterator() {
		Iterator<INSTANCE> iter;
		
		if (datasets.size() > 0) {
			iter = datasets.get(0).iterator();
		} else {
			return null;
		}
		
		for (int i = 1; i < datasets.size(); i++) {
			iter = Iterators.concat(iter, datasets.get(i).iterator());
		}
		
		return iter;
	}

	@Override
	public INSTANCE getRandomInstance() {
		int index = (int) (Math.random() * numInstances());
		
		Iterator<INSTANCE> iter = iterator();
		
		for (int i = 0; i < index; i++) {
			iter.next();
		}
		
		return iter.next();
	}

	@Override
	public int numInstances() {
		int size = 0;
		
		for (Dataset<INSTANCE> dataset : datasets) {
			size += dataset.numInstances();
		}
		
		return size;
	}

}
