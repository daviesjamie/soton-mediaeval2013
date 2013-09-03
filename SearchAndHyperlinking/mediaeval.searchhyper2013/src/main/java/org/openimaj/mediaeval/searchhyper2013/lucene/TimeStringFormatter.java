package org.openimaj.mediaeval.searchhyper2013.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

public class TimeStringFormatter implements Formatter {
	String transcript;
	double[] times;
	
	public TimeStringFormatter(String transcript, String timesString) {
		this.transcript = transcript;
		
		String[] times = timesString.split(" ");
		
		this.times = new double[times.length];
		
		for (int i = 0; i < this.times.length; i++) {
			this.times[i] = Double.parseDouble(times[i]);
		}
	}

	@Override
	public String highlightTerm(String originalText, TokenGroup tokenGroup) {
		if (tokenGroup.getTotalScore() > 0) {
			return ">>>" +
				   originalText + "|" + 
				   times[transcript.substring(0, tokenGroup.getStartOffset())
			             		   .split(" ")
			             		   .length]
			       + "<<<";
		}
		
		return originalText;
	}

	public static List<Float> timesFromString(String timeString) {
		Pattern pattern = Pattern.compile(">>>.*?\\|(\\d+(?:\\.\\d+)?)<<<");
		Matcher matcher = pattern.matcher(timeString);
		
		List<Float> times = new ArrayList<Float>();
		
		while (matcher.find()) {
			times.add(Float.parseFloat(matcher.group(1)));
		}
		
		return times;
	}
	
	public static String getWordAndContextAtTime(String timeString,
												 float time) {
		int index = timeString.indexOf(Float.toString(time));
		
		if (index != -1) {
			final int WIDTH = 100;
			
			return "..." +
				   timeString.substring(
						   Math.max(index - WIDTH, 0),
						   Math.min(index + 15 + WIDTH,
								    timeString.length())) +
				   "...";
		}
		
		return null;
	}

}
