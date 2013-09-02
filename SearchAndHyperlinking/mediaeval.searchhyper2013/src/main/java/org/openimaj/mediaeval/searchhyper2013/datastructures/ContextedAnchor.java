package org.openimaj.mediaeval.searchhyper2013.datastructures;

import org.openimaj.mediaeval.searchhyper2013.util.Time;

public class ContextedAnchor extends Anchor {
	public float contextStartTime;
	public float contextEndTime;
	
	  
	@Override
	public String toString() {
		return anchorID + " | " +
			   anchorName + " | " +
			   Time.StoMS(startTime) + " - " +
			   Time.StoMS(endTime) + " | " +
			   Time.StoMS(contextStartTime) + " - " +
			   Time.StoMS(contextEndTime) + " | " +
			   fileName;
	}
}
