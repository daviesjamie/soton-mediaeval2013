package org.openimaj.mediaeval.searchhyper2013.lucene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;
import org.openimaj.mediaeval.searchhyper2013.datastructures.HighlightedTranscript;
import org.openimaj.mediaeval.searchhyper2013.datastructures.PositionWord;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimedWord;

/** 
 * Formatter for representing highlights as PositionWords in a (de)serialisable
 * manner.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class PositionWordFormatter implements Formatter {
	private static final float PADDING = 60f;
	private static final float PADDING_SCORE = 1f;
	
	public static final String START = ">>>";
	public static final String END = "<<<";	

	@Override
	public String highlightTerm(String originalText, TokenGroup tokenGroup) {
		if (tokenGroup.getTotalScore() > 0) {
			PositionWord word = new PositionWord();
			
			word.word = originalText;
			word.position = tokenGroup.getStartOffset();
			word.score = tokenGroup.getTotalScore();
			
			return START + word.toString() + END;
		} else {
			return originalText;
		}
	}
	
	public static HighlightedTranscript toHighlightedTranscript(String transcript,
																String times,
																String formatted) {
		HighlightedTranscript trans = new HighlightedTranscript(transcript,
																times);
		
		Pattern pattern = Pattern.compile(START + "(.*?)" + END);
		Matcher matcher = pattern.matcher(formatted);
		
		while (matcher.find()) {
			PositionWord pWord = PositionWord.fromString(matcher.group(1));
			
			trans.add(pWord);
		}
		
		Iterator<TimedWord> timedWords = trans.timedWordsIterator();
		Iterator<TimedWord> allWords = trans.allWordsIterator();
		
		// Add some padding.
		if (timedWords.hasNext()) {
			TimedWord first = timedWords.next();
			
			TimedWord currentPaddingFrom = allWords.next();
			
			while (first.time - PADDING > currentPaddingFrom.time) {
				currentPaddingFrom = allWords.next();
			}
			
			while (allWords.hasNext() && currentPaddingFrom.time < first.time) {
				currentPaddingFrom.score = PADDING_SCORE;
				
				PositionWord positionWord =
						trans.timedWordToPositionWord(currentPaddingFrom);
				
				if (positionWord != null) {
					trans.add(positionWord);
				}
				
				currentPaddingFrom = allWords.next();
			}
		}
		
		timedWords = trans.timedWordsIterator();
		allWords = trans.allWordsIterator();
		
		if (timedWords.hasNext()) {
			TimedWord last = timedWords.next();
			
			TimedWord currentPaddingFrom = allWords.next();
			
			while (timedWords.hasNext()) {
				last = timedWords.next();
			}
			
			while (currentPaddingFrom.time < last.time) {
				currentPaddingFrom = allWords.next();
			}
			
			while (allWords.hasNext() && last.time + PADDING > currentPaddingFrom.time) {
				currentPaddingFrom.score = PADDING_SCORE;
				
				trans.add(trans.timedWordToPositionWord(currentPaddingFrom));
				
				currentPaddingFrom = allWords.next();
			}
		}
		
		return trans;
	}
	
	public static List<HighlightedTranscript> toHighlightedTranscripts(String transcript,
																	   String times,
																	   String[] formatted) {
		List<HighlightedTranscript> trans = new ArrayList<HighlightedTranscript>();
		
		for (String part : formatted) {
			trans.add(toHighlightedTranscript(transcript, times, part));
		}
		
		return trans;
	}
}
