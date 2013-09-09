package org.openimaj.mediaeval.searchhyper2013.searcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.openimaj.mediaeval.searchhyper2013.datastructures.HighlightedTranscript;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimedWord;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;

import com.github.wcerfgba.adhocstructures.AddHandler;
import com.github.wcerfgba.adhocstructures.IdentifyRequestHandler;
import com.github.wcerfgba.adhocstructures.SemanticTable;

/**
 * AlphaSearcher with textual query expansion.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class BetaSearcher extends AlphaSearcher {
	float ORIGINAL_QUERY_SCALE_FACTOR = 1f;
	float EXPANSION_RESULTS_SCALE_FACTOR = 0.2f;
	public static final int MAX_EXPANSION_TERMS = 100;

	public BetaSearcher(String runName, IndexReader indexReader) {
		super(runName, indexReader);
	}
	
	@Override
	ResultList _search(Query q) throws Exception {
		if (q == null) {
			return null;
		}
		
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		EnglishAnalyzer englishAnalyzer = new EnglishAnalyzer(LUCENE_VERSION);
		
		StandardQueryParser queryParser =
				new StandardQueryParser(englishAnalyzer);
		org.apache.lucene.search.Query query = 
				queryParser.parse(q.queryText, Field.Text.toString());
		
		// 1. Get synopsis results.
		Filter synopsisFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 Type.Synopsis.toString())));
		TopDocs synopses = indexSearcher.search(query,
												synopsisFilter,
												NUM_SYNOPSIS_RESULTS);

		ScoreDoc[] synopsisScoreDocs = synopses.scoreDocs;
		
		// Normalise synopsis scores.
		float maxScore = 0;
		
		for (ScoreDoc doc : synopsisScoreDocs) {
			maxScore = Math.max(maxScore, doc.score);
		}
		
		for (ScoreDoc doc : synopsisScoreDocs) {
			doc.score = (float) Math.pow(doc.score / maxScore, SYNOPSIS_POWER);
		}
		
		Arrays.sort(synopsisScoreDocs, new Comparator<ScoreDoc>() {
			@Override
			public int compare(ScoreDoc arg0, ScoreDoc arg1) {
				float diff = arg0.score - arg1.score;
				
				if (diff < 0) {
					return -1;
				} else if (diff > 0) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		
		Set<Result> resultSet = new TreeSet<Result>();
		
		// 2. Find results within transcripts.
		for (ScoreDoc synopsis : synopsisScoreDocs) {			
			Document subsDoc =
					LuceneUtils.resolveOtherFromProgramme(synopsis.doc,
														  Type.Subtitles,
														  indexSearcher);
			List<HighlightedTranscript> subsHits = 
					getHighlights(indexSearcher,
								  subsDoc,
								  query,
								  MAX_SUBS_HITS);
			ResultList subsResults =
					super._search(createExpandedQuery(q, subsHits));
			
			for (Result result : subsResults) {
				result.confidenceScore *= EXPANSION_RESULTS_SCALE_FACTOR;
			}
			
			/*Document limsiDoc =
					LuceneUtils.resolveOtherFromProgram(synopsis.doc,
														Type.LIMSI,
														indexSearcher);
			List<HighlightedTranscript> limsiHits = 
					getHighlights(indexSearcher,
								  limsiDoc,
								  query,
								  MAX_LIMSI_HITS);
			ResultList limsiResults =
					super._search(createExpandedQuery(q, limsiHits));
			
			Document liumDoc =
					LuceneUtils.resolveOtherFromProgram(synopsis.doc,
														Type.LIUM,
														indexSearcher);
			List<HighlightedTranscript> liumHits = 
					getHighlights(indexSearcher,
								  liumDoc,
								  query,
								  MAX_LIUM_HITS);
			ResultList liumResults =
					super._search(createExpandedQuery(q, liumHits));
			*/
			if (subsResults != null) {
				resultSet.addAll(subsResults);
			}
			
			/*if (liumResults != null) {
				resultSet.addAll(liumResults);
			}
			
			if (limsiResults != null) {
				resultSet.addAll(limsiResults);
			}*/
		}
		
		ResultList results = new ResultList(q.queryID, runName);
		results.addAll(resultSet);
		
		Collections.sort(results);
		
		System.out.println(results);
		
		return results;
	}
	
	Query createExpandedQuery(Query originalQuery,
							  List<HighlightedTranscript> expansionBase) {
		// Set up ad-hoc data structure.
		final SemanticTable words = new SemanticTable(3);
		
		words.addAddHandler(new AddHandler<Object[]>() {
			
			public boolean handleAdd(Object[] row) {
				String word = ((String) row[0]).toLowerCase().trim();
				Float confidence = (Float) row[1];
				
				Object[] wordRow = words.get(word);
				
				if (wordRow != null) {
					wordRow[1] = (Integer) wordRow[1] + 1;
					wordRow[2] = Math.max((Float) wordRow[2], confidence);
				} else {
					wordRow = new Object[3];
					
					wordRow[0] = word;
					wordRow[1] = 1;
					wordRow[2] = confidence;
				}
				
				words.set(word, wordRow);
				
				return true;
			}
		});
		
		words.addIdentifyRequestHandler(new IdentifyRequestHandler<String, Integer>() {
			
			public Integer handleIdentifyRequest(String identifier) {
				Object[] wordsCol = words.getColumn(0);
				
				for (int i = 0; i < wordsCol.length; i++) {
					if (wordsCol[i].equals(identifier)) {
						return i;
					}
				}
				
				return -1;
			}
		});
		
		// Add words from transcripts.
		for (HighlightedTranscript transcript : expansionBase) {
			Iterator<TimedWord> timedWords = transcript.timedWordsIterator();
			
			while (timedWords.hasNext()) {
				TimedWord timedWord = timedWords.next();
				
				words.add(new Object[] { timedWord.word, timedWord.score });
			}
		}
		
		// Sort table by confidence so we can calculate the weight for the 
		// words in the original query.
		Comparator<Object[]> confidenceComparator = new Comparator<Object[]>() {

			@Override
			public int compare(Object[] arg0, Object[] arg1) {
				if (arg0[2] == null) {
					return 1;
				} else if (arg1[2] == null) {
					return -1;
				}
				
				float diff = (Float) arg1[2] - (Float) arg0[2];
				
				if (diff < 0) {
					return -1;
				} else if (diff > 0) {
					return 1;
				} else {
					return 0;
				}
			}
			
		};
		
		words.sort(confidenceComparator);
		
		float maxConf = 1f;
		
		if (words.getRow(0)[2] != null) {
			maxConf = (Float) words.getRow(0)[2];
		}
		
		for (String word : originalQuery.queryText.split(" ")) {
			words.add(new Object[] { word, ORIGINAL_QUERY_SCALE_FACTOR * maxConf });
		}
		
		words.sort(confidenceComparator);
		
		// Build expansion query.
		StringBuilder query = new StringBuilder();
		
		for (int i = 0; i < MAX_EXPANSION_TERMS && i < words.size(); i++) {
			Object[] row = words.getRow(i);
			
			query.append(row[0] + "^" + ((Integer) row[1] * (Float) row[2]) + " ");
		}
		
		//System.out.println(query.toString());
		
		return new Query(originalQuery.queryID + "_expended",
						 query.toString(),
						 originalQuery.visualCues);
	}
	
	@Override
	public void configure(Float[] settings) {
		super.configure(settings);
		
		ORIGINAL_QUERY_SCALE_FACTOR    = settings[super.numSettings()];
		EXPANSION_RESULTS_SCALE_FACTOR = settings[super.numSettings() + 1];
	}
	
	@Override
	public int numSettings() {
		return super.numSettings() + 2;
	}
}
