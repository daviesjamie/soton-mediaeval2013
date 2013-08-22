package org.openimaj.mediaeval.searchhyper2013;

public abstract class Word {
	public static final String SEP = "~~~";
	
	String word;
	float score;
	
	public String toString() {
		return word + SEP + score;
	}
}