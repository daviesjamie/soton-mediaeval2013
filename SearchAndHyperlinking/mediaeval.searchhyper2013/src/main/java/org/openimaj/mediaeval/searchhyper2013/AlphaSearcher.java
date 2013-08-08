package org.openimaj.mediaeval.searchhyper2013;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.util.Version;

/**
 * The Alpha searcher works as follows:
 *   1. Find relevant programs by searching for query text within synopses.
 *   2. Find query text hits within each transcription of these programs.
 *   3. Search over segments of transcripts to establish segment relevance.
 *   4. Return relevant segments.
 *   
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class AlphaSearcher implements Searcher {
	static final Version LUCENE_VERSION = Version.LUCENE_43;
	
	static final int NUM_SYNOPSIS_RESULTS = 3;
	static final int MAX_SUBS_HITS = 100;
	static final int MAX_LIMSI_HITS = 100;
	static final int MAX_LIUM_HITS = 100;
	static final float SUBS_SCALE_FACTOR = 0.3f;
	static final float LIMSI_SCALE_FACTOR = 1f;
	static final float LIUM_SCALE_FACTOR = 1f;
	float MIN_LENGTH = 60 * 3;
	float MAX_LENGTH = 60 * 15;
	
	IndexReader indexReader;
	String runName;
	
	public AlphaSearcher(String runName, IndexReader indexReader) {
		this.indexReader = indexReader;
		this.runName = runName;
	}
	
	@Override
	public ResultList search(Query q) throws SearcherException {
		try {
			return _search(q);
		} catch (Exception e) {
			throw new SearcherException(e);
		}
	}
	
	ResultList _search(Query q) throws Exception {
		if (q == null) {
			return null;
		}
		
		//System.out.println("Running AlphaSearcher with query text: " + 
		//				   q.queryText);
		
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

		Set<Result> resultSet = new HashSet<Result>();
		
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
					ResultList.fromHighlightedTranscripts(q.queryID,
														  runName,
														  subsDoc.get(Field.Program.toString()),
														  subsHits,
														  SUBS_SCALE_FACTOR,
														  MIN_LENGTH,
														  MAX_LENGTH);
			
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
					ResultList.fromHighlightedTranscripts(q.queryID,
														  runName,
														  limsiDoc.get(Field.Program.toString()),
														  limsiHits,
														  LIMSI_SCALE_FACTOR,
														  MIN_LENGTH,
														  MAX_LENGTH);
			
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
					ResultList.fromHighlightedTranscripts(q.queryID,
														  runName,
														  liumDoc.get(Field.Program.toString()),
														  liumHits,
														  LIUM_SCALE_FACTOR,
														  MIN_LENGTH,
														  MAX_LENGTH);
			
			resultSet.addAll(subsResults);
			resultSet.addAll(liumResults);
			resultSet.addAll(limsiResults);
		}
		
		ResultList results = new ResultList(q.queryID, runName);
		results.addAll(resultSet);
		
		Collections.sort(results);
		
		return results;
	}

	List<HighlightedTranscript> getHighlights(IndexSearcher indexSearcher,
													  Document doc,
													  org.apache.lucene.search.Query query,
													  int maxHits) 
												throws IOException, InvalidTokenOffsetsException {
		//System.out.println("Getting highlights for: " + 
		//				   doc.get(Field.Program.toString()) + 
		//				   " (" + doc.get(Field.Type.toString()) + ")");
		
		Highlighter highlighter =
				new Highlighter(
					new PositionWordFormatter(),
					new DefaultEncoder(),
					new QueryTermScorer(query));
		
		String[] frags = 
			highlighter.getBestFragments(new EnglishAnalyzer(LUCENE_VERSION),
										 Field.Text.toString(),
										 doc.get(Field.Text.toString()),
										 maxHits);
		
		List<HighlightedTranscript> hits = 
				PositionWordFormatter.toHighlightedTranscripts(doc.get(Field.Text.toString()),
															   doc.get(Field.Times.toString()),
															   frags);
		
		return hits;
	}
}
