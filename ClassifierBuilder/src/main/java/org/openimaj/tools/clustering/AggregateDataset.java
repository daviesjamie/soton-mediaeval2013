package org.openimaj.tools.clustering;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openimaj.data.dataset.Dataset;

import com.google.common.collect.Iterators;

/**
 * Aggregate Dataset that merges its component Datasets back-to-back.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <INSTANCE>
 */
public class AggregateDataset<DATASET extends Dataset<INSTANCE>, INSTANCE> implements Dataset<INSTANCE> {
	Set<DATASET> datasets;
	
	public AggregateDataset() {
		datasets = new HashSet<DATASET>();
	}
	
	public AggregateDataset(Collection<DATASET> datasets) {
		this();
		
		addDatasets(datasets);
	}
	
	public boolean addDataset(DATASET dataset) {
		return datasets.add(dataset);
	}
	
	public boolean addDatasets(Collection<? extends DATASET> datasets) {
		return this.datasets.addAll(datasets);
	}
	
	public Set<DATASET> getSet() {
		return datasets;
	}

	@Override
	public Iterator<INSTANCE> iterator() {
		Iterator<INSTANCE> iter = Iterators.emptyIterator();
		
		for (DATASET dataset : datasets) {
			iter = Iterators.concat(iter, dataset.iterator());
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
