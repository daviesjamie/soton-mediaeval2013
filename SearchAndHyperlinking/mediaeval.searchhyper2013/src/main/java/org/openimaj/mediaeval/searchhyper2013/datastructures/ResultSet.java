package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ResultSet implements Set<Result> {
	final float FUDGE;
	
	Set<Result> results;
	
	public ResultSet(float mergeWindow) {
		results = new HashSet<Result>();
		
		FUDGE = mergeWindow;
	}
	
	@Override
	public boolean add(Result newResult) {
		Set<Result> overlappers = new HashSet<Result>();
		
		for (Result result : results) {
			if (!result.fileName.equals(newResult.fileName)) {
				continue;
			}
			
			if (result.startTime < newResult.startTime - FUDGE &&
				result.endTime   > newResult.startTime - FUDGE) {
				overlappers.add(result);
			} else if (result.startTime < newResult.endTime + FUDGE &&
					   result.endTime   > newResult.endTime + FUDGE) {
				overlappers.add(result);
			} else if (result.startTime > newResult.startTime - FUDGE &&
					   result.endTime   < newResult.endTime + FUDGE) {
				overlappers.add(result);
			}
		}
		
		if (overlappers.isEmpty()) {
			return results.add(newResult);
		}
		
		Result mergedResult = new Result();
		mergedResult.startTime = Float.MAX_VALUE;
		mergedResult.jumpInPoint = 0;
		mergedResult.endTime = 0;
		mergedResult.confidenceScore = 0;
		mergedResult.fileName = newResult.fileName;
		
		overlappers.add(newResult);
		
		for (Result overlapper : overlappers) {
			mergedResult.startTime =
					Math.min(mergedResult.startTime, overlapper.startTime);
			mergedResult.jumpInPoint += overlapper.jumpInPoint;
			mergedResult.endTime =
					Math.max(mergedResult.endTime, overlapper.endTime);
			mergedResult.confidenceScore += overlapper.confidenceScore;
		}
		
		mergedResult.jumpInPoint /= overlappers.size();
		
		results.removeAll(overlappers);
		
		return results.add(mergedResult);
	}

	@Override
	public boolean addAll(Collection<? extends Result> arg0) {
		for (Result result : arg0) {
			add(result);
		}
		
		return true;
	}

	@Override
	public void clear() {
		results.clear();
	}

	@Override
	public boolean contains(Object o) {
		return results.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return results.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return results.isEmpty();
	}

	@Override
	public Iterator<Result> iterator() {
		return results.iterator();
	}

	@Override
	public boolean remove(Object o) {
		return results.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return results.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return results.retainAll(c);
	}

	@Override
	public int size() {
		return results.size();
	}

	@Override
	public Object[] toArray() {
		return results.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return results.toArray(a);
	}
}
