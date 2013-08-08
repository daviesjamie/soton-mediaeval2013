package org.openimaj.mediaeval.searchhyper2013;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.openimaj.util.pair.Pair;

import com.github.wcerfgba.adhocstructures.AddHandler;
import com.github.wcerfgba.adhocstructures.IdentifyRequestHandler;
import com.github.wcerfgba.adhocstructures.SemanticTable;

public class QueryExpandingAlphaSearcher extends AlphaSearcher {

	public QueryExpandingAlphaSearcher(String runName, IndexReader indexReader) {
		super(runName, indexReader);
	}

	@Override
	ResultList _search(Query q) throws Exception {
		System.out.println("Running QueryExpandingAlphaSearcher with query " + 
						   "text: " + q.queryText);
		
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

		ResultList results = new ResultList(q.queryID, runName);
		
		// 2. Find results within transcripts.
		for (ScoreDoc synopsis : synopses.scoreDocs) {
			Document subsDoc =
					LuceneUtils.resolveOtherFromProgram(synopsis.doc,
														Type.Subtitles,
														indexSearcher);
			List<HighlightedTranscript> subsHits = 
					getHighlights(indexSearcher,
								  subsDoc,
								  query,
								  MAX_SUBS_HITS);
			ResultList subsResults =
					super.search(createExpandedQuery(q, subsHits));
			
			Document limsiDoc =
					LuceneUtils.resolveOtherFromProgram(synopsis.doc,
														Type.LIMSI,
														indexSearcher);
			List<HighlightedTranscript> limsiHits = 
					getHighlights(indexSearcher,
								  limsiDoc,
								  query,
								  MAX_LIMSI_HITS);
			ResultList limsiResults =
					super.search(createExpandedQuery(q, limsiHits));
			
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
					super.search(createExpandedQuery(q, liumHits));
			
			results.addAll(subsResults);
			results.addAll(liumResults);
			results.addAll(limsiResults);
		}
		
		return results;
	}
	
	static Query createExpandedQuery(Query originalQuery,
							  List<HighlightedTranscript> expansionBase) {
		final SemanticTable words = new SemanticTable(3);
		
		words.addAddHandler(new AddHandler<Object[]>() {
			
			public boolean handleAdd(Object[] row) {
				String word = (String) row[0];
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
				
				int row = 0;
				while (!wordsCol[row].equals(identifier)) {
					row++;
				}
				
				return row;
			}
		});
		
		
	}
}
