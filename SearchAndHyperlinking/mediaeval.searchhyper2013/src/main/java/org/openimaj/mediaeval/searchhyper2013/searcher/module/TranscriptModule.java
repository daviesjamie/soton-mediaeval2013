package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.TimeStringFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.Time;

public class TranscriptModule implements SearcherModule {

	Type TRANSCRIPT_TYPE;
	double TRANSCRIPT_WEIGHT = 0.15;
	double TRANSCRIPT_POWER = 2;
	double TRANSCRIPT_WIDTH = 60;
	double FRAGMENT_DENSITY_FACTOR = 0.1;
	
	Version LUCENE_VERSION = Version.LUCENE_43;
	
	QueryParser queryParser;
	IndexSearcher indexSearcher;
	Analyzer analyzer;
	Directory spellDir;
	TimelineFactory timelineFactory;
	
	public TranscriptModule(IndexSearcher indexSearcher,
							Type transcriptType,
							Analyzer analyzer,
							Directory spellDir,
							TimelineFactory timelineFactory) {
		this.indexSearcher = indexSearcher;
		this.analyzer = analyzer;
		this.spellDir = spellDir;
		this.timelineFactory = timelineFactory;
		
		TRANSCRIPT_TYPE = transcriptType;
		
		queryParser = new ComplexPhraseQueryParser(
						LUCENE_VERSION,
						Field.Text.toString(),
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
		String query = LuceneUtils.fixSpelling(
							QueryParser.escape(
									ChannelFilterModule.removeChannel(
											q.queryText)),
							spellDir);
		
		System.out.println(query);
		
		org.apache.lucene.search.Query luceneQuery = 
				queryParser.parse(query);
		
		System.out.println(luceneQuery);
		
		Filter transcriptFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 TRANSCRIPT_TYPE.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
							indexSearcher.search(luceneQuery,
												 transcriptFilter,
												 10));
		
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (ScoreDoc doc : hits) {
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			//System.out.println("Transcript hit: " + luceneDocument.get(Field.Program.toString()));
			
			String id = luceneDocument.get(Field.Program.toString());
			
			Timeline programmeTimeline =
					timelines.getTimelineWithID(id);
			
			if (programmeTimeline == null) {
				programmeTimeline =	timelineFactory.makeTimeline(id);
			}
			
			if (programmeTimeline.containsInstanceOf(
					TitleModule.TitleFunction.class)) {
				Document synopsisDocument =
						LuceneUtils.getSynopsisForProgramme(id, indexSearcher);
				String[] titleWords =
						synopsisDocument.get(Field.Title.toString()).split(" ");
				
				String[] queryParts = query.split(" ");
				
				StringBuilder sb = new StringBuilder();
				
				query:
				for (String queryPart : queryParts) {
					for (String titleWord : titleWords) {
						if (queryPart.toLowerCase()
									 .contains(titleWord.toLowerCase())) {
							continue query;
						}
					}
					
					sb.append(queryPart + " ");
				}
				
				luceneQuery = queryParser.parse(sb.toString().trim());
			}
			
			String transcript = luceneDocument.get(Field.Text.toString());
			
			Highlighter timeHighlighter = 
					new Highlighter(
						new TimeStringFormatter(
							transcript,
							luceneDocument.get(Field.Times.toString()),
							indexSearcher.getIndexReader(),
							doc.doc),
						new DefaultEncoder(),
						new QueryScorer(luceneQuery));
			TokenStream tokenStream =
					analyzer.tokenStream(Field.Text.toString(),
										 new StringReader(transcript));
			
			/*String timeString = 
					timeHighlighter.getBestFragments(tokenStream,
													 transcript,
													 1000000, " ");*/
			String[] frags = timeHighlighter.getBestFragments(tokenStream,
											 				  transcript,
											 				  100);
			
			for (String frag : frags) {
				Map<Float, Double> times = 
						TimeStringFormatter.timesFromString(frag);
				
				float start = Float.MAX_VALUE;
				float end = 0;
				double score = 0;
				
				for (Float time : times.keySet()) {
					if (time > programmeTimeline.getEndTime() + 30) {
						continue;
					}
					
					start = Math.min(start, time);
					end = Math.max(end, time);
					score += times.get(time);
				}
				
				if (start == Float.MAX_VALUE) {
					continue;
				}
				
				score += FRAGMENT_DENSITY_FACTOR * (times.size() - 1);
				
				TranscriptFunction function = 
						new TranscriptFunction(TRANSCRIPT_WEIGHT * doc.score *
									 			 Math.pow(score,
									 					  TRANSCRIPT_POWER),
									 		   (end - start) / 2,
									 		   ((end - start) + TRANSCRIPT_WIDTH)
									 		   		/ 3d);
				programmeTimeline.addFunction(function);
				
				function.addJustification(
						"Transcript matched at " + Time.StoMS(start) + " to " + 
						Time.StoMS(end) + " with score " + score + " and " + 
						"match: " + frag);
			}
			
			//System.out.println(timeString + "\n");
			
			tokenStream.close();
			/*
			Map<Float, Double> times =
					TimeStringFormatter.timesFromString(timeString);
			
			for (Float time : times.keySet()) {
				
				// Don't add if it's past the end of the programme.
				if (time > programmeTimeline.getEndTime() + 30) {
					continue;
				}
				
				TranscriptFunction function = 
						new TranscriptFunction(TRANSCRIPT_WEIGHT * doc.score *
									 			 Math.pow(times.get(time),
									 					  TRANSCRIPT_POWER),
									 		   time,
									 		   TRANSCRIPT_WIDTH / 3d);
				programmeTimeline.addFunction(function);
				
				function.addJustification(
						"Transcript matched at " + Time.StoMS(time) + " with score " + 
						times.get(time) + " and " + "match: " +
						TimeStringFormatter.getWordAndContextAtTime(timeString,
																	time));
			}
			*/
			if (programmeTimeline.numFunctions() > 0) {
				timelines.add(programmeTimeline);
			}
		}
		
		return timelines;
	}
	
	public class TranscriptFunction extends Gaussian
									implements JustifiedTimedFunction {
		List<String> justifications;
		
		double mean;
		
		public TranscriptFunction(double norm, double mean, double sigma) {
			super(norm, mean, sigma);
			
			this.mean = mean;
			
			justifications = new ArrayList<String>();
		}
		
		public boolean addJustification(String justification) {
			return justifications.add(justification);
		}
		
		public List<String> getJustifications() {
			return justifications;
		}
		
		public float getTime() {
			return (float) mean;
		}
		
		@Override
		public String toString() {
			return "Transcript function @ " + Time.StoMS((float) mean);
		}
	}
}
