package org.openimaj.mediaeval.searchhyper2013;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
	
	static final int NUM_SYNOPSIS_RESULTS = 5;
	static final int MAX_SUBS_HITS = 1000;
	static final int MAX_LIMSI_HITS = 1000;
	static final int MAX_LIUM_HITS = 1000;
	float SUBS_SCALE_FACTOR = 1f;
	float LIMSI_SCALE_FACTOR = 0f;
	float LIUM_SCALE_FACTOR = 0f;
	float MIN_LENGTH = 60 * 1;
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

		ScoreDoc[] scoreDocs = synopses.scoreDocs;
		Arrays.sort(scoreDocs, new Comparator<ScoreDoc>() {

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
		for (ScoreDoc synopsis : scoreDocs) {
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
														  SUBS_SCALE_FACTOR * synopsis.score,
														  MIN_LENGTH,
														  MAX_LENGTH);
			
			//System.out.println(subsResults);
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
														  MAX_LENGTH);*/
			
			resultSet.addAll(subsResults);
			//resultSet.addAll(liumResults);
			//resultSet.addAll(limsiResults);
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
		
		
		TokenStream tokenStream =
				new EnglishAnalyzer(LUCENE_VERSION)
							.tokenStream(Field.Text.toString(),
										 new StringReader(
											doc.get(Field.Text.toString())));
		
		TextFragment[] frag = 
				highlighter.getBestTextFragments(tokenStream,
									 doc.get(Field.Text.toString()),
									 false,
									 maxHits);

	    //Get text
	    ArrayList<String> fragTexts = new ArrayList<String>();
	    for (int i = 0; i < frag.length; i++)
	    {
	      if ((frag[i] != null) && (frag[i].getScore() > 0))
	      {
	        fragTexts.add(frag[i].toString());
	      }
	    }
	    
	    tokenStream.close();

		List<HighlightedTranscript> hits = 
				PositionWordFormatter.toHighlightedTranscripts(doc.get(Field.Text.toString()),
															   doc.get(Field.Times.toString()),
															   fragTexts.toArray(new String[0]));
		
		return hits;
	}

	@Override
	public void configure(Float[] settings) {
		SUBS_SCALE_FACTOR = settings[0];
		LIMSI_SCALE_FACTOR = settings[1];
		LIUM_SCALE_FACTOR = settings[2];
		MIN_LENGTH = settings[3];
		MAX_LENGTH = settings[4];
	}
}
