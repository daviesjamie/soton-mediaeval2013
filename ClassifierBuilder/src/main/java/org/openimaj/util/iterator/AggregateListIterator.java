package org.openimaj.util.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Immutable ListIterator aggregate of ListIterators.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class AggregateListIterator<T> implements ListIterator<T> {
	List<ListIterator<T>> nextIters;
	List<ListIterator<T>> prevIters;
	ListIterator<T> curIter;
	
	int index = 0;
	
	public AggregateListIterator(Collection<? extends ListIterator<T>> collection) {
		nextIters = new ArrayList<ListIterator<T>>(collection);
		curIter = nextIters.remove(0);
	}
	
	public boolean addIterator(ListIterator<T> iter) {
		return nextIters.add(iter);
	}

	@Override
	public void add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasNext() {
		if (curIter.hasNext()) {
			return true;
		}
		
		for (int i = 0; i < nextIters.size(); i++) {
			if (nextIters.get(i).hasNext()) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean hasPrevious() {
		if (curIter.hasPrevious()) {
			return true;
		}
		
		for (int i = prevIters.size() - 1; i >= 0; i--) {
			if (prevIters.get(i).hasPrevious()) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public T next() {
		while (!curIter.hasNext()) {
			if (!nextIters.isEmpty()) {
				prevIters.add(curIter);
				curIter = nextIters.remove(0);
			} else {
				throw new NoSuchElementException();
			}
		}
		
		index++;
		
		return curIter.next();
	}

	@Override
	public int nextIndex() {
		return index;
	}

	@Override
	public T previous() {
		while (!curIter.hasPrevious()) {
			if (!prevIters.isEmpty()) {
				nextIters.add(curIter);
				curIter = prevIters.remove(prevIters.size() - 1);
			} else {
				throw new NoSuchElementException();
			}
		}
		
		index--;
		
		return curIter.previous();
	}

	@Override
	public int previousIndex() {
		return index - 1;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(T e) {
		throw new UnsupportedOperationException();
	}

}
