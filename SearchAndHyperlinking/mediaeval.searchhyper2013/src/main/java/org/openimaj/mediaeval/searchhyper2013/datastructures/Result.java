package org.openimaj.mediaeval.searchhyper2013.datastructures;

import org.openimaj.mediaeval.searchhyper2013.util.Time;

/**
 * Represents a result from a search.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Result implements Comparable<Result> {
	public String fileName;
	public float startTime;
	public float endTime;
	public float jumpInPoint;
	public float confidenceScore;
	
	public float length() {
		return endTime - startTime;
	}

	@Override
	public int compareTo(Result o) {
		float confDiff = o.confidenceScore - confidenceScore;
		
		if (confDiff < 0) {
			return -1;
		} else if (confDiff > 0) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Overridden to eliminate confidence score being a deciding factor for 
	 * equality.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(endTime);
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + Float.floatToIntBits(jumpInPoint);
		result = prime * result + Float.floatToIntBits(startTime);
		return result;
	}

	/**
	 * Overridden to eliminate confidence score being a deciding factor for 
	 * equality.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Result other = (Result) obj;
		if (Float.floatToIntBits(endTime) != Float
				.floatToIntBits(other.endTime))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (Float.floatToIntBits(jumpInPoint) != Float
				.floatToIntBits(other.jumpInPoint))
			return false;
		if (Float.floatToIntBits(startTime) != Float
				.floatToIntBits(other.startTime))
			return false;
		return true;
	}
	
	public String toString() {
		return fileName + " " + 
			   Time.StoMS(startTime) + " " + 
			   Time.StoMS(endTime) + " " + 
			   Time.StoMS(jumpInPoint) + " " + 
			   confidenceScore;
	}
}