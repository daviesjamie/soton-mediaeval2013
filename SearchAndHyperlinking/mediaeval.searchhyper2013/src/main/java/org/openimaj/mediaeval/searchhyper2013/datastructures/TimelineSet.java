package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.HashSet;

public class TimelineSet extends HashSet<Timeline> {
	
	public TimelineSet() {
		super();
	}
	
	public TimelineSet(TimelineSet other) {
		super(other);
	}
	
	/* (non-Javadoc)
	 * @see java.util.HashSet#add(java.lang.Object)
	 */
	@Override
	public boolean add(Timeline e) {
		for (Timeline timeline : this) {
			if (timeline.equals(e)) {
				timeline.mergeIn(e);
				
				return true;
			}
		}
		
		return super.add(e);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Timeline timeline : this) {
			sb.append(timeline.toString() + "\n\n");
		}
		
		return sb.toString();
	}
}
