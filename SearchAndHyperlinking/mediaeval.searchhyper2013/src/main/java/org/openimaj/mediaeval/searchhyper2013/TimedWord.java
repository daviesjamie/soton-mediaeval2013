package org.openimaj.mediaeval.searchhyper2013;

public class TimedWord extends Word {
	float time;
	
	public String toString() {
		return super.toString() + SEP + time;
	}
	
	public static TimedWord fromString(String string) {
		String[] components = string.split(SEP, 3);
		
		TimedWord word = new TimedWord();
		
		word.word = components[0];
		word.score = Float.parseFloat(components[1]);
		word.time = Float.parseFloat(components[2]);
		
		return word;
	}
}
