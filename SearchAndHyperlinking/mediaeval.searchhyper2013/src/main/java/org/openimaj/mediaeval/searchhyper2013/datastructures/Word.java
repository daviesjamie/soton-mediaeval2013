package org.openimaj.mediaeval.searchhyper2013.datastructures;

/**
 * Represents a highlighted Word in a HighlightedTranscript.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public abstract class Word {
	public static final String SEP = "~~~";
	
	public String word;
	public float score;
	
	public String toString() {
		return word + SEP + score;
	}
}