package org.openimaj.mediaeval.searchhyper2013;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
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
	public static ScoredDocuments retreiveTopDocs(TopDocs topDocs, 
												  IndexReader indexReader)
											throws IOException {
		ScoredDocuments docs = new ScoredDocuments(topDocs.totalHits);
		
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
	
	public static Document resolveOtherFromProgram(int doc, Type type, IndexSearcher searcher) throws IOException {
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
}
