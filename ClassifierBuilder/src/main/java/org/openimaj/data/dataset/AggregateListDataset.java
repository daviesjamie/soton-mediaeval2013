package org.openimaj.data.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.openimaj.data.dataset.ListDataset;
import org.openimaj.util.iterator.AggregateListIterator;

/**
 * Immutable ListDataset aggregate of ListDatasets.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <DATASET>
 * @param <INSTANCE>
 */
public class AggregateListDataset<INSTANCE> extends AggregateDataset<ListDataset<INSTANCE>, INSTANCE>
		implements ListDataset<INSTANCE> {
	
	List<ListDataset<INSTANCE>> datasets;
	
	public AggregateListDataset() {
		datasets = new ArrayList<ListDataset<INSTANCE>>();
	}
	
	@Override
	public boolean addDataset(ListDataset<INSTANCE> dataset) {
		return datasets.add(dataset);
	}
	
	@Override
	public boolean addDatasets(Collection<? extends ListDataset<INSTANCE>> datasets) {
		return this.datasets.addAll(datasets);
	}
	
	@Override
	public boolean add(INSTANCE e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, INSTANCE element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends INSTANCE> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends INSTANCE> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		for (ListDataset<INSTANCE> dataset : datasets) {
			if (dataset.contains(o)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public INSTANCE get(int index) {
		if (index < 0 || index > size() - 1) {
			throw new IndexOutOfBoundsException();
		}
		
		// Determine collection item is in.
		ListDataset<INSTANCE> dataset = null;
		
		Iterator<ListDataset<INSTANCE>> datasetIter = datasets.iterator();
		int startNext = 0;
		int startCur = startNext;
		
		while (startNext <= index && datasetIter.hasNext()) {
			dataset = datasetIter.next();
			startCur = startNext;
			startNext += dataset.numInstances();
		}
		
		return dataset.get(index - startCur);
	}

	@Override
	public int indexOf(Object o) {
		for (int i = 0; i < size(); i++) {
			if (get(i).equals(o)) {
				return i;
			}
		}
		
		return -1;
	}

	@Override
	public boolean isEmpty() {
		for (ListDataset<INSTANCE> d : datasets) {
			if (!d.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public int lastIndexOf(Object o) {
		for (int i = size() - 1; i <= 0; i--) {
			if (get(i).equals(o)) {
				return i;
			}
		}
		
		return -1;
	}

	@Override
	public ListIterator<INSTANCE> listIterator() {
		Set<ListIterator<INSTANCE>> iterSet = new HashSet<ListIterator<INSTANCE>>();
		
		for (ListDataset<INSTANCE> d : datasets) {
			iterSet.add(d.listIterator());
		}
		
		return new AggregateListIterator<INSTANCE>(iterSet);
	}

	@Override
	public ListIterator<INSTANCE> listIterator(int index) {
		ListIterator<INSTANCE> iter = listIterator();
		
		for (int i = 0; i < index; i++) {
			iter.next();
		}
		
		return iter;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public INSTANCE remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public INSTANCE set(int index, INSTANCE element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		int size = 0;
		
		for (ListDataset<INSTANCE> d : datasets) {
			size += d.size();
		}
		
		return size;
	}

	@Override
	public List<INSTANCE> subList(int fromIndex, int toIndex) {
		ListIterator<INSTANCE> iter = listIterator(fromIndex);
		
		List<INSTANCE> list = new ArrayList<INSTANCE>(toIndex - fromIndex);
		
		for (int i = fromIndex; i < toIndex; i++) {
			list.add(iter.next());
		}
		
		return list;
	}

	@Override
	public Object[] toArray() {
		return subList(0, size()).toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return subList(0, size()).toArray(a);
	}

	@Override
	public INSTANCE getInstance(int index) {
		return subList(0, size()).get(index);
	}

}
