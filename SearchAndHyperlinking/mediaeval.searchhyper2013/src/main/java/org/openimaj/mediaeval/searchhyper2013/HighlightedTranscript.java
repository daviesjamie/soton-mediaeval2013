package org.openimaj.mediaeval.searchhyper2013;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
}
