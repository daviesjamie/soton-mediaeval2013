package org.openimaj.tools.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;

import com.google.common.collect.Iterators;

/**
 * Presents an immutable view of a collection of GroupedDataset's by 
 * aggregating their inner ListDatasets.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class AggregateGroupedDataset<KEY, DATASET extends ListDataset<INSTANCE>, INSTANCE> 
				extends AggregateDataset<GroupedDataset<KEY, DATASET, INSTANCE>, INSTANCE> 
				implements GroupedDataset<KEY, AggregateListDataset<INSTANCE>, INSTANCE> {
	
	Map<KEY, Set<ListDataset<INSTANCE>>> datasets;
	Map<KEY, AggregateListDataset<INSTANCE>> aggregateDatasets;
	
	public AggregateGroupedDataset() {
		datasets = new HashMap<KEY, Set<ListDataset<INSTANCE>>>();
		aggregateDatasets = new HashMap<KEY, AggregateListDataset<INSTANCE>>();
	}
	
	@Override
	public boolean addDataset(GroupedDataset<KEY, DATASET, INSTANCE> dataset) {
		for (KEY key : dataset.keySet()) {
			if (datasets.containsKey(key)) {
				datasets.get(key).add(dataset.get(key));
				aggregateDatasets.get(key).addDataset(dataset.get(key));
			} else {
				Set<ListDataset<INSTANCE>> set = new HashSet<ListDataset<INSTANCE>>();
				set.add(dataset.get(key));
				datasets.put(key, set);
				
				AggregateListDataset<INSTANCE> aggregateDataset =
					new AggregateListDataset<INSTANCE>();
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
			for (ListDataset<INSTANCE> dataset : datasets.get(key)) {
				iter = Iterators.concat(iter, dataset.iterator());
			}
		}
		
		return iter;
	}

	@Override
	public int numInstances() {
		int size = 0;
		
		for (KEY key : datasets.keySet()) {
			for (ListDataset<INSTANCE> dataset : datasets.get(key)) {
				size += dataset.numInstances();
			}
		}
		
		return size;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
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
	public Set<java.util.Map.Entry<KEY, AggregateListDataset<INSTANCE>>> entrySet() {
		return aggregateDatasets.entrySet();
	}

	@Override
	public AggregateListDataset<INSTANCE> get(Object key) {
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
	public AggregateListDataset<INSTANCE> put(KEY key,
			AggregateListDataset<INSTANCE> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(
			Map<? extends KEY, ? extends AggregateListDataset<INSTANCE>> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AggregateListDataset<INSTANCE> remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return datasets.size();
	}

	@Override
	public Collection<AggregateListDataset<INSTANCE>> values() {
		return aggregateDatasets.values();
	}

	@Override
	public AggregateListDataset<INSTANCE> getInstances(KEY key) {
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
