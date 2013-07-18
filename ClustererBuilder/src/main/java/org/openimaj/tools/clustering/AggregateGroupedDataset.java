package org.openimaj.tools.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;

import com.google.common.collect.Iterators;

/**
 * Aggregates its member GroupedDatasets together to present them as a single 
 * GroupedDataset.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class AggregateGroupedDataset<KEY, DATASET extends Dataset<INSTANCE>, INSTANCE> 
				extends AggregateDataset<GroupedDataset<KEY, DATASET, INSTANCE>, INSTANCE> 
				implements GroupedDataset<KEY, AggregateDataset<DATASET, INSTANCE>, INSTANCE> {
	
	Map<KEY, Set<DATASET>> datasets;
	Map<KEY, AggregateDataset<DATASET, INSTANCE>> aggregateDatasets;
	
	public AggregateGroupedDataset() {
		datasets = new HashMap<KEY, Set<DATASET>>();
		aggregateDatasets = new HashMap<KEY, AggregateDataset<DATASET, INSTANCE>>();
	}
	
	@Override
	public boolean addDataset(GroupedDataset<KEY, DATASET, INSTANCE> dataset) {
		for (KEY key : dataset.keySet()) {
			if (datasets.containsKey(key)) {
				datasets.get(key).add(dataset.get(key));
				aggregateDatasets.get(key).addDataset(dataset.get(key));
			} else {
				Set<DATASET> set = new HashSet<DATASET>();
				set.add(dataset.get(key));
				datasets.put(key, set);
				
				AggregateDataset<DATASET, INSTANCE> aggregateDataset =
					new AggregateDataset<DATASET, INSTANCE>();
				aggregateDataset.addDataset(dataset.get(key));
				aggregateDatasets.put(key, aggregateDataset);
			}
		}
		
		return true;
	}
	
	@Override
	public Iterator<INSTANCE> iterator() {		
		Iterator<INSTANCE> iter = Iterators.emptyIterator();
		
		for (KEY key : datasets.keySet()) {
			for (DATASET dataset : datasets.get(key)) {
				iter = Iterators.concat(iter, dataset.iterator());
			}
		}
		
		return iter;
	}

	@Override
	public int numInstances() {
		int size = 0;
		
		for (KEY key : datasets.keySet()) {
			for (DATASET dataset : datasets.get(key)) {
				size += dataset.numInstances();
			}
		}
		
		return size;
	}

	@Override
	public void clear() {
		datasets.clear();
		aggregateDatasets.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return datasets.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (KEY key : datasets.keySet()) {
			if (datasets.get(key).contains(value)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public Set<java.util.Map.Entry<KEY, AggregateDataset<DATASET, INSTANCE>>> entrySet() {
		return aggregateDatasets.entrySet();
	}

	@Override
	public AggregateDataset<DATASET, INSTANCE> get(Object key) {
		return aggregateDatasets.get(key);
	}

	@Override
	public boolean isEmpty() {
		return datasets.isEmpty();
	}

	@Override
	public Set<KEY> keySet() {
		return datasets.keySet();
	}

	@Override
	public AggregateDataset<DATASET, INSTANCE> put(KEY key,
			AggregateDataset<DATASET, INSTANCE> value) {
		AggregateDataset<DATASET, INSTANCE> old = aggregateDatasets.get(key);
		
		aggregateDatasets.put(key, value);
		datasets.put(key, value.getSet());
		
		return old;
	}

	@Override
	public void putAll(
			Map<? extends KEY, ? extends AggregateDataset<DATASET, INSTANCE>> m) {
		for (KEY key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public AggregateDataset<DATASET, INSTANCE> remove(Object key) {
		AggregateDataset<DATASET, INSTANCE> removed = aggregateDatasets.remove(key);
		datasets.remove(key);
		
		return removed;
	}

	@Override
	public int size() {
		return datasets.size();
	}

	@Override
	public Collection<AggregateDataset<DATASET, INSTANCE>> values() {
		return aggregateDatasets.values();
	}

	@Override
	public AggregateDataset<DATASET, INSTANCE> getInstances(KEY key) {
		return get(key);
	}

	@Override
	public Set<KEY> getGroups() {
		return keySet();
	}

	@Override
	public INSTANCE getRandomInstance(KEY key) {
		return aggregateDatasets.get(key).getRandomInstance();
	}

	

}
