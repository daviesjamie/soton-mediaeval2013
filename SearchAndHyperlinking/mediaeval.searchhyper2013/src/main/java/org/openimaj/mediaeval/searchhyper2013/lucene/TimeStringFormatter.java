package org.openimaj.mediaeval.searchhyper2013.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.util.Version;

public class TimeStringFormatter implements Formatter {
	String transcript;
	double[] times;
	IndexReader indexReader;
	int docID;
	
	StandardQueryParser queryParser;
	
	public TimeStringFormatter(String transcript,
							   String timesString,
							   IndexReader indexReader,
							   int docID) {
		this.transcript = transcript;
		
		String[] times = timesString.split(" ");
		
		this.times = new double[times.length];
		
		for (int i = 0; i < this.times.length; i++) {
			this.times[i] = Double.parseDouble(times[i]);
		}
		
		this.indexReader = indexReader;
		this.docID = docID;
		
		queryParser = new StandardQueryParser(new EnglishAnalyzer(Version.LUCENE_43));
	}

	@Override
	public String highlightTerm(String originalText, TokenGroup tokenGroup) {
		if (tokenGroup.getTotalScore() > 0) {
			double score = 0;

			try {
				Set<Term> terms = new HashSet<Term>();
				queryParser.parse(originalText, Field.Text.toString())
						   .extractTerms(terms);
				Term term = (Term) terms.toArray()[0];
			
				DocsEnum docs =
						MultiFields.getTermDocsEnum(indexReader,
													MultiFields.getLiveDocs(
															indexReader),
													Field.Text.toString(),
													term.bytes());
				
				int current;
				while ((current = docs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
					if (current == docID) {
						score = 1d / docs.freq();
						break;
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (QueryNodeException e) {
				throw new RuntimeException(e);
			}
			
			return ">>>" +
				   originalText + "|" + 
				   times[transcript.substring(0, tokenGroup.getStartOffset())
			             		   .split(" ")
			             		   .length] + "|" +
			       score
			       + "<<<";
		}
		
		return originalText;
	}

	public static Map<Float, Double> timesFromString(String timeString) {
		Pattern pattern = Pattern.compile(">>>.*?\\|(\\d+(?:\\.\\d+)?)\\|(\\d+(?:\\.\\d+)?)<<<");
		Matcher matcher = pattern.matcher(timeString);
		
		Map<Float, Double> times = new HashMap<Float, Double>();
		
		while (matcher.find()) {
			times.put(Float.parseFloat(matcher.group(1)),
					  Double.parseDouble(matcher.group(2)));
		}
		
		// Normalise.
		/*double maxScore = 0;
		
		for (Double score : times.values()) {
			maxScore = Math.max(maxScore,  score);
		}
		
		for (Float time : times.keySet()) {
			times.put(time, times.get(time) / maxScore);
		}*/
		
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
