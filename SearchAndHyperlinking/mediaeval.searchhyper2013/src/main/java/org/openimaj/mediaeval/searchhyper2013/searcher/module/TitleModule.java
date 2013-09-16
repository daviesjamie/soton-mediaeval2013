package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.function.Constant;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;

public class TitleModule implements SearcherModule {
	Version LUCENE_VERSION = Version.LUCENE_43;
	
	double MINIMUM_SCORE = 0.8;
	double TITLE_WEIGHT = 30;
	double TITLE_POWER = 0.5;
	
	StandardQueryParser queryParser;
	IndexSearcher indexSearcher;
	Directory spellDir;
	TimelineFactory timelineFactory;
	
	public TitleModule(IndexSearcher indexSearcher,
					   Directory spellDir,
					   TimelineFactory timelineFactory) {
		this.indexSearcher = indexSearcher;
		this.spellDir = spellDir;
		this.timelineFactory = timelineFactory;
		
		queryParser = new StandardQueryParser(
							new EnglishAnalyzer(LUCENE_VERSION));
	}
	
	@Override
	public TimelineSet search(Query q,
							  TimelineSet currentSet)
													throws SearcherException {
		try {
			return _search2(q, currentSet);
		} catch (Exception e) {
			throw new SearcherException(e);
		}
	}

	public TimelineSet _search2(Query q,
							   TimelineSet currentSet) throws Exception {
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (Timeline timeline : timelines) {
			Document synopsis =
					LuceneUtils.getTypeForProgramme(timeline.getID(),
													Type.Synopsis,
														indexSearcher);
			List<String> common = LuceneUtils.getCommonTokens(LuceneUtils.fixQuery(
					QueryParser.escape(q.queryText),
					spellDir),
				synopsis.get(Field.Title.toString()));
			if (!common.isEmpty()) {
				timeline.scaleMultiplier(1 + (TITLE_WEIGHT *
						Math.pow(1,
								 TITLE_POWER)));

				timeline.addJustification(
				"Title match : " + synopsis.get(Field.Title.toString()));
			}
		}
		
		return timelines;
	}
	
	public TimelineSet _search(Query q,
							   TimelineSet currentSet)
														throws Exception {
		org.apache.lucene.search.Query luceneQuery = 
				queryParser.parse(
						LuceneUtils.fixQuery(
									QueryParser.escape(q.queryText),
									spellDir),
						Field.Title.toString());
		Filter synopsisFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 Type.Synopsis.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
							indexSearcher.search(luceneQuery,
												 synopsisFilter,
												 1000000));
		
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (ScoreDoc doc : hits) {
			if (doc.score < MINIMUM_SCORE) {
				continue;
			}
			
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			//System.out.println("Title hit: " + luceneDocument.get(Field.Program.toString()));
			
			//System.out.println(luceneDocument.get(Field.Title.toString()));
			
			Timeline programmeTimeline =
					timelineFactory.makeTimeline(
							luceneDocument.get(Field.Program.toString()));
			
			/*TitleFunction function = new TitleFunction(TITLE_WEIGHT *
															Math.pow(doc.score,
													   TITLE_POWER));
			programmeTimeline.addFunction(function);*/
			
			programmeTimeline.scaleMultiplier(1 + (TITLE_WEIGHT *
													Math.pow(doc.score,
															 TITLE_POWER)));
			
			programmeTimeline.addJustification(
					"Title match with score " + doc.score + 
					": " + luceneDocument.get(Field.Title.toString()));
			
			timelines.add(programmeTimeline);
		}
		
		return timelines;
	}

	/*public class TitleFunction extends Constant
			  					  implements JustifiedTimedFunction {
		List<String> justifications;
		
		public TitleFunction(double c) {
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
			return "Title function";
		}
	}*/
}
