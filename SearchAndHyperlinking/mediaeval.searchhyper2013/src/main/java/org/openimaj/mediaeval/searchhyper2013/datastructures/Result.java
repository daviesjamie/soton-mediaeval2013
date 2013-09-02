package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.math.BigDecimal;

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
	public double confidenceScore;
	
	public Result() { }
	
	public Result(Result other) {
		fileName = other.fileName;
		startTime = other.startTime;
		endTime = other.endTime;
		jumpInPoint = other.jumpInPoint;
		confidenceScore = other.confidenceScore;
	}
	
	public float length() {
		return endTime - startTime;
	}

	@Override
	public int compareTo(Result o) {
		return new Double(o.confidenceScore).compareTo(confidenceScore);
	}

	@Override
	public String toString() {
		return fileName + " " +
			   Time.StoMS(startTime) + " " +
			   Time.StoMS(endTime) + " " +
			   Time.StoMS(jumpInPoint) + " " +
			   confidenceScore;
	}
}
