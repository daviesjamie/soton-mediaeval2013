package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.HashSet;

public class TimelineSet extends HashSet<Timeline> {

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
}
