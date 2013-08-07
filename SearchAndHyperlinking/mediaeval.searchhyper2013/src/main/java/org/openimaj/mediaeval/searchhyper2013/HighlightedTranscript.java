package org.openimaj.mediaeval.searchhyper2013;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.search.highlight.TokenGroup;

/**
 * Represents a transcript that certain words 'highlighted', with a score. 
 * Provides methods for resolving a list of times in the transcript when these 
 * marks are.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class HighlightedTranscript extends ArrayList<PositionWord> {
	public static final String SEP = "___";
	
	private String transcript;
	private float[] times;
	
	public HighlightedTranscript(String transcript, String times) {
		super();
		
		this.transcript = transcript;
		
		String[] stringTimes = times.split(" ");
		
		this.times = new float[stringTimes.length];
		
		for (int i = 0; i < this.times.length; i++) {
			this.times[i] = Float.parseFloat(stringTimes[i]);
		}
	}
	
	/*public String toString() {
		String string = "";
		
		for (PositionWord word : this) {
			string += word.toString() + SEP;
		}
		
		if (string.length() != 0) {
			string = string.substring(0, string.length() - SEP.length() - 1);
		}
		
		return string;
	}*/
	
	/*public static HighlightedTranscript fromString(String string) {
		String[] components = string.split(SEP);
		
		HighlightedTranscript transcript = new HighlightedTranscript();
		
		for (String component : components) {
			transcript.add(PositionWord.fromString(component));
		}
		
		return transcript;
	}*/
	
	/*public List<TimedWord> resolveTimes(String transcript, String timesString) {
		String[] timesStrings = timesString.split(" ");
		
		float[] times = new float[timesStrings.length];
		
		for (int i = 0; i < times.length; i++) {
			times[i] = Float.parseFloat(timesStrings[i]);
		}
		
		List<TimedWord> words = new ArrayList<TimedWord>(this.size());
		
		for (PositionWord word : this) {
			String[] cutTrans = transcript.substring(0, word.position).split(" ");
			
			TimedWord tWord = new TimedWord();
			
			tWord.word = word.word;
			tWord.score = word.score;
			tWord.time = times[cutTrans.length - 1];
			
			words.add(tWord);
		}
		
		return words;
	}*/

	/*public static List<HighlightedTranscript> fromStrings(String[] strings) {
		List<HighlightedTranscript> list = 
				new ArrayList<HighlightedTranscript>(strings.length);
		
		for (String string : strings) {
			list.add(fromString(string));
		}
		
		return list;
	}*/

	private float resolveTime(int index) {
		PositionWord pWord = get(index);
		
		int wordNo = transcript.subSequence(0, pWord.position)
							   .toString()
							   .split(" ")
							   .length - 1;
		
		return times[wordNo];
	}
	
	public float startTime() {
		return resolveTime(0);
	}
	
	public float endTime() {
		return resolveTime(size() - 1);
	}
	
	public float confidence() {
		float confidence = 0f;
		
		for (Word word : this) {
			confidence += word.score;
		}
		
		return confidence;
	}

	public float length() {
		return endTime() - startTime();
	}
	
	
}
