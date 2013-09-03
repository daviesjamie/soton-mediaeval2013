package org.openimaj.mediaeval.searchhyper2013.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;

public abstract class LuceneUtils {
	
	static final Version LUCENE_VERSION = Version.LUCENE_43;
	
	public static List<String> getCommonTokens(String queryString, String searchString)
			  throws IOException, QueryNodeException, InvalidTokenOffsetsException {
		
		EnglishAnalyzer analyzer = new EnglishAnalyzer(LUCENE_VERSION);
		
		StandardQueryParser queryParser =
				new StandardQueryParser(analyzer);
		org.apache.lucene.search.Query query = 
				queryParser.parse(queryString, "foo");
		
		Highlighter highlighter = new Highlighter(new Formatter() {

			@Override
			public String highlightTerm(String originalText,
					TokenGroup tokenGroup) {
				
				if (tokenGroup.getTotalScore() > 0) {
					return ">>>" + originalText + "|" + tokenGroup.getTotalScore() + "<<<";
				} else {
					return originalText;
				}
				
			}}, new DefaultEncoder(), new QueryTermScorer(query));
		
		TokenStream tokenStream =
				analyzer.tokenStream("foo",
									 new StringReader(searchString));
		
		TextFragment[] frag = 
				highlighter.getBestTextFragments(tokenStream,
									 			 searchString,
									 			 false,
									 			 1000);

	    //Get text
		Pattern pattern = Pattern.compile(">>>(.*?)<<<");
		
	    List<String> hits = new ArrayList<String>();
	    
	    for (int i = 0; i < frag.length; i++)
	    {
	      if ((frag[i] != null) && (frag[i].getScore() > 0))
	      {
	        Matcher matcher = pattern.matcher(frag[i].toString());
	        
	        while (matcher.find()) {
	        	String[] parts = matcher.group(1).split("\\|");
	        	hits.add(parts[0]);
	        }
	      }
	    }
	    
	    tokenStream.close();
	    
	    return hits;
	}
	
	/**
	 * Converts TopDocs to Map<Document, Float>.
	 * 
	 * @param topDocs
	 * @param indexReader
	 * @return
	 * @throws IOException
	 */
	public static Map<Document, Float> retreiveTopDocs(TopDocs topDocs, 
												  	   IndexReader indexReader)
												  			throws IOException {
		Map<Document, Float> docs = new HashMap<Document, Float>(topDocs.totalHits);
		
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = indexReader.document(scoreDoc.doc);
			
			docs.put(doc, scoreDoc.score);
		}
		
		return docs;
	}

	public static SpanQuery textQueryToSpanQuery(String queryText, int slop) {
		String[] words = queryText.split(" ");
		
		SpanQuery[] termQueries = new SpanQuery[words.length];
		
		for (int i = 0; i < termQueries.length; i++) {
			termQueries[i] = new SpanTermQuery(
								new Term(Field.Text.toString(),
										 words[i]));
		}
		
		return new SpanNearQuery(termQueries, slop, false);
	}
	
	/**
	 * Extracts Spans.
	 * 
	 * @param spanQuery
	 * @param atomicReader
	 * @return
	 * @throws IOException
	 */
	public static Spans getSpans(SpanQuery spanQuery, AtomicReader atomicReader) throws IOException {
		AtomicReaderContext context = atomicReader.getContext();
		Bits acceptDocs = null;
		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
		
		Set<Term> terms = new HashSet<Term>();
		
		spanQuery.rewrite(atomicReader);
		spanQuery.extractTerms(terms);
		
		for (Term term : terms) {
			termContexts.put(term, TermContext.build(context, term, true));
		}
		
		return spanQuery.getSpans(context, acceptDocs, termContexts);
	}
	
	/**
	 * Resolves a Document with a specified type given the docID for some other 
	 * document for the same programme.
	 * 
	 * @param doc
	 * @param type
	 * @param searcher
	 * @return
	 * @throws IOException
	 */
	public static Document resolveOtherFromProgramme(int doc,
													 Type type,
													 IndexSearcher searcher)
															throws IOException {
		Document baseDoc = searcher.doc(doc);
		
		String program = baseDoc.get(Field.Program.toString());
		
		BooleanQuery progTypeQuery = new BooleanQuery();
		progTypeQuery.add(
				new TermQuery(new Term(Field.Program.toString(), program)),
				BooleanClause.Occur.MUST);
		progTypeQuery.add(
				new TermQuery(new Term(Field.Type.toString(), type.toString())),
				BooleanClause.Occur.MUST);
		
		TopDocs docs = searcher.search(progTypeQuery, 1);
		
		return searcher.doc(docs.scoreDocs[0].doc);
	}

	public static ScoreDoc[] normaliseTopDocs(TopDocs search) {
		ScoreDoc[] scoreDocs = Arrays.copyOf(search.scoreDocs,
											 search.scoreDocs.length);
		
		double maxScore = 0;
		
		for (ScoreDoc scoreDoc : scoreDocs) {
			maxScore = Math.max(maxScore, scoreDoc.score);
		}
		
		for (ScoreDoc scoreDoc : scoreDocs) {
			scoreDoc.score /= maxScore;
		}
		
		return scoreDocs;
	}
}
