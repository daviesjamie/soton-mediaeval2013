package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ResultSet implements Set<Result> {
	Set<Result> results;
	
	public ResultSet() {
		results = new HashSet<Result>();
	}

	@Override
	public boolean add(Result arg0) {
		if (size() == 0) {
			results.add(arg0);
			
			return true;
		}
		
		HashSet<Result> newResults = new HashSet<Result>();
		
		boolean merged = false;
		
		for (Result result : results) {
			if (!result.fileName.equals(arg0.fileName) || merged) {
				newResults.add(result);
				
				continue;
			}
			
			if (result.startTime <= arg0.startTime) {
				Result resultCopy = new Result(result);
				
				if (arg0.endTime <= result.endTime + 0.001) {
					resultCopy.confidenceScore = 
						resultCopy.confidenceScore + arg0.confidenceScore;
					
					merged = true;
				} else if (arg0.startTime <= result.endTime + 0.001) {
					resultCopy.endTime = arg0.endTime;
					resultCopy.confidenceScore = 
						resultCopy.confidenceScore + arg0.confidenceScore;
					
					merged = true;
				}
				
				newResults.add(resultCopy);
			} else {
				Result resultCopy = new Result(arg0);
				
				if (result.endTime <= arg0.endTime + 0.001) {
					resultCopy.confidenceScore = 
						resultCopy.confidenceScore + result.confidenceScore;
					
					merged = true;
				} else if (result.startTime <= arg0.endTime + 0.001) {
					resultCopy.endTime = result.endTime;
					resultCopy.confidenceScore = 
						resultCopy.confidenceScore + result.confidenceScore;
					
					merged = true;
				} else {
					newResults.add(result);
					
					continue;
				}
				
				newResults.add(resultCopy);
			}
		}
		
		if (!merged) {
			newResults.add(arg0);
		}
		
		results = newResults;
		
		return true;
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
