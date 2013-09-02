package org.openimaj.mediaeval.searchhyper2013.searcher;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.util.Version;
import org.openimaj.mediaeval.searchhyper2013.datastructures.HighlightedTranscript;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.PositionWordFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;

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
	static final int NUM_TITLE_RESULTS = 100;
	static final int MAX_SUBS_HITS = 1000;
	static final int MAX_LIMSI_HITS = 1000;
	static final int MAX_LIUM_HITS = 1000;
	float TITLE_BOOST = 10f;
	float SYNOPSIS_POWER = 2f;
	float SUBS_SCALE_FACTOR = 0.5f;
	float SUBS_POWER = 2f;
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
		
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		EnglishAnalyzer englishAnalyzer = new EnglishAnalyzer(LUCENE_VERSION);
		
		StandardQueryParser queryParser =
				new StandardQueryParser(englishAnalyzer);
		
		String queryString = 
				"(" + Field.Title.toString() + ":(" + q.queryText + ")^" + TITLE_BOOST + " OR " + 
				Field.Text.toString() + ":(" + q.queryText + "))";
		
		System.out.println("Query string: " + queryString + "\n");
		
		org.apache.lucene.search.Query query = 
				queryParser.parse(
						queryString,
						Field.Text.toString());
		
		// 1. Get synopsis results.
		Filter synopsisFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 Type.Synopsis.toString())));
		TopDocs synopses = indexSearcher.search(query,
												synopsisFilter,
												NUM_SYNOPSIS_RESULTS);

		List<ScoreDoc> synopsisScoreDocs =
				new ArrayList<ScoreDoc>(synopses.scoreDocs.length);
		
		for (ScoreDoc scoreDoc : synopses.scoreDocs) {
			synopsisScoreDocs.add(scoreDoc);
		}
		
		// Normalise synopsis scores.
		float maxScore = 0;
		
		for (ScoreDoc doc : synopsisScoreDocs) {
			maxScore = Math.max(maxScore, doc.score);
		}
		
		for (ScoreDoc doc : synopsisScoreDocs) {
			doc.score = (float) Math.pow(doc.score / maxScore, SYNOPSIS_POWER);
		}

		Collections.sort(synopsisScoreDocs, new Comparator<ScoreDoc>() {
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
		
		// Filter results on wrong channels.
		Pattern bbcPattern =
				Pattern.compile("bbc\\s*(one|1|two|2|three|3|four|4)?",
								Pattern.CASE_INSENSITIVE);
		Matcher bbcMatcher = bbcPattern.matcher(q.queryText);
		
		String channelString = null;
		
		while (bbcMatcher.find()) {
			String channel = bbcMatcher.group(1);
			
			if (channel != null) {
				if (channel.equals("1")) {
					channel = "one";
				} else if (channel.equals("2")) {
					channel = "two";
				} else if (channel.equals("3")) {
					channel = "three";
				} else if (channel.equals("4")){
					channel = "four";
				}
				
				channelString = "bbc" + channel;
			}
		}
		
		if (channelString != null) {
			Iterator<ScoreDoc> iter = synopsisScoreDocs.iterator();
			
			while (iter.hasNext()) {
				ScoreDoc doc = iter.next();
				
				if (!indexReader.document(doc.doc)
								.get(Field.Program.toString())
								.contains(channelString)) {
					iter.remove();
				}
			}
		}
		
		/*for (ScoreDoc scoreDoc : synopsisScoreDocs) {
			Document doc = indexReader.document(scoreDoc.doc);
			
			System.out.println("Synopsis hit: " + doc.get(Field.Program.toString()) + " | " + scoreDoc.score);
			
			// HIGHLIGHT
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
										 100);

		    //Get text
		    StringBuilder sb = new StringBuilder();
		    for (int i = 0; i < frag.length; i++)
		    {
		      if ((frag[i] != null) && (frag[i].getScore() > 0))
		      {
		        sb.append(frag[i].toString() + "||");
		      }
		    }
		    
		    tokenStream.close();
		    
		    System.out.println(sb.toString() + "\n");
		}*/
		
		System.out.println();
		
		ResultSet resultSet = new ResultSet();
		
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
					ResultList.fromHighlightedTranscripts(q.queryID,
														  runName,
														  subsDoc.get(Field.Program.toString()),
														  subsHits,
														  SUBS_SCALE_FACTOR,
														  SUBS_POWER,
														  synopsis.score,
														  MIN_LENGTH,
														  MAX_LENGTH);
			
			System.out.println(subsResults + "\n");
			
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
		System.out.println("Getting highlights for: " + 
						   doc.get(Field.Program.toString()) + 
						   " (" + doc.get(Field.Type.toString()) + ")");
		
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
	    
	    for (String fragt : fragTexts) {
	   // 	System.out.println(fragt + "||");
	    }

		List<HighlightedTranscript> hits = 
				PositionWordFormatter.toHighlightedTranscripts(doc.get(Field.Text.toString()),
															   doc.get(Field.Times.toString()),
															   fragTexts.toArray(new String[0]));
		
		return hits;
	}

	@Override
	public void configure(Float[] settings) {
		TITLE_BOOST = settings[0];
		SYNOPSIS_POWER = settings[1];
		SUBS_SCALE_FACTOR = settings[2];
		SUBS_POWER = settings[3];
		LIMSI_SCALE_FACTOR = settings[4];
		LIUM_SCALE_FACTOR = settings[5];
		MIN_LENGTH = settings[6];
		MAX_LENGTH = settings[7];
	}
	
	@Override
	public int numSettings() {
		return 8;
	}
}
