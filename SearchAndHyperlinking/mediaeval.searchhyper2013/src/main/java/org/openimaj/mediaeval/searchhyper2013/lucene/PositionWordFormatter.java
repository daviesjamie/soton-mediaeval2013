package org.openimaj.mediaeval.searchhyper2013.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;
import org.openimaj.mediaeval.searchhyper2013.datastructures.HighlightedTranscript;
import org.openimaj.mediaeval.searchhyper2013.datastructures.PositionWord;

/** 
 * Formatter for representing highlights as PositionWords in a (de)serialisable
 * manner.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class PositionWordFormatter implements Formatter {
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
			trans.add(PositionWord.fromString(matcher.group(1)));
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
