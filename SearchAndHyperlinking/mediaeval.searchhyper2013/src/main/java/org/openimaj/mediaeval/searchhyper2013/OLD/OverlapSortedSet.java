package org.openimaj.mediaeval.searchhyper2013.OLD;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class OverlapSortedSet<OUTER extends Overlappable<OUTER, INNER> & Comparable<OUTER> & Collection<? extends Comparable<INNER>>, INNER> implements SortedSet<OUTER> {
	SortedSet<OUTER> set = new TreeSet<OUTER>();
	
	@Override
	public boolean add(OUTER arg0) {
		SortedSet<OUTER> newSet = new TreeSet<OUTER>();
		
		// Find elements that are overlapped by the new element. Non-overlapped 
		// elements can be added straight to the new set.
		SortedSet<OUTER> overlapped = new TreeSet<OUTER>();
		
		for (OUTER obj : set) {
			if (obj.overlappedBy(arg0)) {
				overlapped.add(obj);
			} else {
				newSet.add(obj);
			}
		}
		
		// If there are no overlaps we can add the new element to the set
		// immediately.
		if (overlapped.isEmpty()) {
			newSet.add(arg0);
		} else {
			// For each overlapped element, we must resolve the overlap and 
			// clip the result to the overlapped set, then we can add it to the 
			// new set.
			for (OUTER obj : overlapped) {
				Collection<OUTER> resolved = obj.splitOverlap(arg0);
				newSet.addAll(obj.clipCollection(resolved));
			}
		}
		
		// The new element has now been integrated into the set and all 
		// overlaps have been resolved, so we can update the internal set.
		set = newSet;
		
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends OUTER> arg0) {
		for (OUTER obj : arg0) {
			add(obj);
		}
		
		return true;
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return set.contains(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return set.containsAll(arg0);
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public Iterator<OUTER> iterator() {
		return set.iterator();
	}

	@Override
	public boolean remove(Object arg0) {
		// TODO: This needs a re-implementation.
		return set.remove(arg0);
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		for (Object o : arg0) {
			remove(o);
		}
		
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <OUTER> OUTER[] toArray(OUTER[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Comparator<? super OUTER> comparator() {
		return set.comparator();
	}

	@Override
	public OUTER first() {
		return set.first();
	}

	@Override
	public SortedSet<OUTER> headSet(OUTER arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OUTER last() {
		return set.last();
	}

	@Override
	public SortedSet<OUTER> subSet(OUTER arg0, OUTER arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedSet<OUTER> tailSet(OUTER arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
