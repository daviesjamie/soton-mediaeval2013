package org.openimaj.mediaeval.searchhyper2013.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

public abstract class LuceneUtils {
	
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
		ScoreDoc[] scoreDocs = search.scoreDocs;
		
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
