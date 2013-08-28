package org.openimaj.mediaeval.searchhyper2013.datastructures;

import org.openimaj.mediaeval.searchhyper2013.util.Time;

public class Anchor {
	public String anchorID;
	public String anchorName;
	public float startTime;
	public float endTime;
	public String fileName;
	
	@Override
	public String toString() {
		return anchorID + " | " +
			   anchorName + " | " +
			   Time.StoMS(startTime) + " | " +
			   Time.StoMS(endTime) + " | " +
			   fileName;
	}
}
