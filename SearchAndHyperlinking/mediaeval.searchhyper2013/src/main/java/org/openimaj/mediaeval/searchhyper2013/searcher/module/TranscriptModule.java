package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.util.Version;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.TimeStringFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.Time;

public class TranscriptModule implements SearcherModule {

	Type TRANSCRIPT_TYPE;
	double TRANSCRIPT_WEIGHT = 0.1;
	double TRANSCRIPT_POWER = 2;
	double TRANSCRIPT_WIDTH = 60;
	
	Version LUCENE_VERSION = Version.LUCENE_43;
	
	StandardQueryParser queryParser;
	IndexSearcher indexSearcher;
	Analyzer analyzer;
	
	public TranscriptModule(IndexSearcher indexSearcher,
							Type transcriptType,
							Analyzer analyzer) {
		this.indexSearcher = indexSearcher;
		this.analyzer = analyzer;
		
		TRANSCRIPT_TYPE = transcriptType;
		
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
			
			Document synopsis =
					LuceneUtils.resolveOtherFromProgramme(doc.doc,
														  Type.Synopsis,
														  indexSearcher);
			
			Timeline programmeTimeline =
				new Timeline(luceneDocument.get(Field.Program.toString()),
							 Float.parseFloat(
									 synopsis.get(Field.Length.toString())));
			
			String transcript = luceneDocument.get(Field.Text.toString());
			
			Highlighter timeHighlighter = 
					new Highlighter(
						new TimeStringFormatter(
							transcript,
							luceneDocument.get(Field.Times.toString())),
						new DefaultEncoder(),
						new QueryTermScorer(luceneQuery));
			TokenStream tokenStream =
					analyzer.tokenStream(Field.Text.toString(),
										 new StringReader(transcript));
			
			String timeString = 
					timeHighlighter.getBestFragments(tokenStream,
													 transcript,
													 1000000, " ");
			
			//System.out.println(timeString + "\n");
			
			tokenStream.close();
			
			List<Float> times = TimeStringFormatter.timesFromString(timeString);
			
			for (float time : times) {
				
				// Don't add if it's past the end of the programme.
				if (time > programmeTimeline.endTime + 30) {
					continue;
				}
				
				TranscriptFunction function = 
						new TranscriptFunction(TRANSCRIPT_WEIGHT *
									 			 Math.pow(doc.score,
									 					  TRANSCRIPT_POWER),
									 		   time,
									 		   TRANSCRIPT_WIDTH / 3d);
				programmeTimeline.addFunction(function);
				
				function.addJustification(
						"Transcript matched at " + Time.StoMS(time) + " with score " + 
						doc.score + " and " + "match: " +
						TimeStringFormatter.getWordAndContextAtTime(timeString,
																	time));
			}
			
			timelines.add(programmeTimeline);
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
