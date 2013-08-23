package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a transcript that certain words 'highlighted', with a score. 
 * Provides methods for resolving a list of times in the transcript when these 
 * marks are.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class HighlightedTranscript extends ArrayList<PositionWord> {
	private static final long serialVersionUID = 6501575487464269849L;

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
	
	/**
	 * Copy constructor.
	 * 
	 * @param other
	 */
	public HighlightedTranscript(HighlightedTranscript other) {
		super(other);
		
		transcript = other.transcript;
		times = Arrays.copyOf(other.times, other.times.length);
	}
	
	public HighlightedTranscript(String transcript, float[] times) {
		super();
		
		this.transcript = transcript;
		this.times = times;
	}

	/**
	 * Resolves the time information for a given PositionWord in this 
	 * HighlightedTranscript.
	 * 
	 * @param word
	 * @return The TimedWord version of the given PositionWord.
	 */
	private TimedWord positionWordToTimedWord(PositionWord word) {
		TimedWord timedWord = new TimedWord();
		
		timedWord.score = word.score;
		timedWord.time = times[transcript.subSequence(0, word.position)
		                       			 .toString()
		                       			 .split(" ")
		                       			 .length - 1];
		timedWord.word = word.word;
		
		return timedWord;
	}
	
	public float startTime() {
		return positionWordToTimedWord(get(0)).time;
	}
	
	public float endTime() {
		return positionWordToTimedWord(get(size() - 1)).time;
	}
	
	/**
	 * 
	 * @return Sum of confidences of all PositionWords in this 
	 * 		   HighlightedTranscript.
	 */
	public float confidence() {
		float confidence = 0f;
		
		for (Word word : this) {
			confidence += word.score;
		}
		
		return confidence;
	}

	public float length() {
		if (size() == 0) {
			return 0f;
		} else {
			return endTime() - startTime();
		}
	}
	
	public Iterator<TimedWord> timedWordsIterator() {
		return new Iterator<TimedWord>() {
			Iterator<PositionWord> iter = iterator();
			
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public TimedWord next() {
				return positionWordToTimedWord(iter.next());
			}

			@Override
			public void remove() {
				iter.remove();
			}
			
		};
	}
	
	/**
	 * Partitions this HighlightedTranscript into a List<HighlightedTranscript> 
	 * by dividing into chunks not exceeding maxLength.]
	 * 
	 * @param maxLength
	 * @return
	 */
	public List<HighlightedTranscript> splitByMaxLength(float maxLength) {
		List<HighlightedTranscript> splits = 
				new ArrayList<HighlightedTranscript>();
		
		HighlightedTranscript current = null;
		
		for (PositionWord word : this) {
			if (current == null) {
				current = new HighlightedTranscript(transcript, times);
			}
			
			if (current.length() < maxLength) {
				current.add(word);
			} else {
				splits.add(current);
				current = null;
			}
		}
		
		if (current != null) {
			splits.add(current);
		}
		
		return splits;
	}
}
