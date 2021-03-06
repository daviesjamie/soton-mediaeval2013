package org.openimaj.mediaeval.searchhyper2013.datastructures;

/**
 * Represents a word with a given Position in a HighlgihtedTranscript.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class PositionWord extends Word {
	public int position;
	
	public String toString() {
		return super.toString() + SEP + position;
	}
	
	public static PositionWord fromString(String string) {
		String[] components = string.split(SEP, 3);
		
		PositionWord word = new PositionWord();

		word.word = components[0];
		word.score = Float.parseFloat(components[1]);
		word.position = Integer.parseInt(components[2]);
		
		return word;
	}
}

