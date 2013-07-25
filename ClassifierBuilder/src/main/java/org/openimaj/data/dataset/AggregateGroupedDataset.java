package org.openimaj.data.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;

import com.google.common.collect.Iterators;

/**
 * Presents an immutable view of a collection of GroupedDataset's by 
 * aggregating their inner Datasets.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class AggregateGroupedDataset<KEY, DATASET extends Dataset<INSTANCE>, INSTANCE, AGGREGATE extends AggregateDataset<DATASET, INSTANCE>> 
				extends AggregateDataset<GroupedDataset<KEY, DATASET, INSTANCE>, INSTANCE> 
				implements GroupedDataset<KEY, AGGREGATE, INSTANCE> {
	
	Map<KEY, List<Dataset<INSTANCE>>> datasets;
	Map<KEY, AGGREGATE> aggregateDatasets;
	Class<AGGREGATE> aggregateClass;

	public AggregateGroupedDataset() throws Exception {
		throw new Exception("An aggregate class must be specified.");
	}
	
	@SuppressWarnings("unchecked")
	public AggregateGroupedDataset(Class<? extends AGGREGATE> aggregateClass) {
		datasets = new HashMap<KEY, List<Dataset<INSTANCE>>>();
		aggregateDatasets = new HashMap<KEY, AGGREGATE>();
		this.aggregateClass = (Class<AGGREGATE>) aggregateClass;
	}
	
	@Override
	public boolean addDataset(GroupedDataset<KEY, DATASET, INSTANCE> dataset) {
		for (KEY key : dataset.keySet()) {
			if (datasets.containsKey(key)) {
				datasets.get(key).add(dataset.get(key));
				aggregateDatasets.get(key).addDataset(dataset.get(key));
			} else {
				List<Dataset<INSTANCE>> set = new ArrayList<Dataset<INSTANCE>>();
				set.add(dataset.get(key));
				datasets.put(key, set);
				
				AGGREGATE aggregateDataset;
				try {
					aggregateDataset = aggregateClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
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
			for (Dataset<INSTANCE> dataset : datasets.get(key)) {
				iter = Iterators.concat(iter, dataset.iterator());
			}
		}
		
		return iter;
	}

	@Override
	public int numInstances() {
		int size = 0;
		
		for (KEY key : datasets.keySet()) {
			for (Dataset<INSTANCE> dataset : datasets.get(key)) {
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
	public Set<java.util.Map.Entry<KEY, AGGREGATE>> entrySet() {
		return aggregateDatasets.entrySet();
	}

	@Override
	public AGGREGATE get(Object key) {
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
	public AGGREGATE put(KEY key,
			AGGREGATE value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(
			Map<? extends KEY, ? extends AGGREGATE> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AGGREGATE remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return datasets.size();
	}

	@Override
	public Collection<AGGREGATE> values() {
		return aggregateDatasets.values();
	}

	@Override
	public AGGREGATE getInstances(KEY key) {
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
