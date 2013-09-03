package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.function.Constant;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;

public class SynopsisModule implements SearcherModule {
	Version LUCENE_VERSION = Version.LUCENE_43;
	
	double SYNOPSIS_WEIGHT = 10;
	double SYNOPSIS_POWER = 2;
	
	StandardQueryParser queryParser;
	IndexSearcher indexSearcher;
	
	public SynopsisModule(IndexSearcher indexSearcher) {
		this.indexSearcher = indexSearcher;
		
		queryParser = new StandardQueryParser(
						new EnglishAnalyzer(LUCENE_VERSION));
	}
	
	@Override
	public TimelineSet search(Query q, TimelineSet currentSet)
													throws SearcherException {
		try {
			return _search(q, currentSet);
		} catch (Exception e) {
			throw new SearcherException(e);
		}
	}

	public TimelineSet _search(Query q, TimelineSet currentSet)
														throws Exception {
		org.apache.lucene.search.Query luceneQuery = 
				queryParser.parse(q.queryText, Field.Text.toString());
		Filter synopsisFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 Type.Synopsis.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
							indexSearcher.search(luceneQuery,
												 synopsisFilter,
												 10));
		
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (ScoreDoc doc : hits) {
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			//System.out.println("Synopsis hit: " + luceneDocument.get(Field.Program.toString()));
			
			//System.out.println(luceneDocument.get(Field.Text.toString()));
			
			Timeline programmeTimeline =
				new Timeline(luceneDocument.get(Field.Program.toString()),
							 Float.parseFloat(
								luceneDocument.get(Field.Length.toString())));
			SynopsisFunction function = 
					new SynopsisFunction(SYNOPSIS_WEIGHT *
											Math.pow(doc.score,
													 SYNOPSIS_POWER));
			programmeTimeline.addFunction(function);
			
			List<String> commonWords =
				LuceneUtils.getCommonTokens(q.queryText,
											luceneDocument.get(Field.Text.toString()));
			StringBuilder sb = new StringBuilder();
			
			sb.append("Synopsis matched on ");
			
			for (String word : commonWords) {
				sb.append("'" + word + "', ");
			}
			
			function.addJustification(
					sb.toString().substring(0, sb.toString().length() - 2) + 
					" with score " + doc.score);
			
			timelines.add(programmeTimeline);
		}
		
		return timelines;
	}

	public class SynopsisFunction extends Constant
								  implements JustifiedTimedFunction {
		List<String> justifications;
		
		public SynopsisFunction(double c) {
			super(c);
			
			justifications = new ArrayList<String>();
		}
		public boolean addJustification(String justification) {
			return justifications.add(justification);
		}
		
		public List<String> getJustifications() {
			return justifications;
		}
		
		public float getTime() {
			return 0;
		}
		
		@Override
		public String toString() {
			return "Synopsis function";
		}
	}
}
