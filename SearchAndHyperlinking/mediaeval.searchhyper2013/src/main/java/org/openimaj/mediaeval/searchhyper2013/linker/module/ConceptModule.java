package org.openimaj.mediaeval.searchhyper2013.linker.module;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenGroup;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Concept;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Concepts;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Frame;
import org.openimaj.mediaeval.searchhyper2013.datastructures.JustifiedTimedFunction;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.linker.LinkerException;
import org.openimaj.mediaeval.searchhyper2013.lucene.EnglishSynonymAnalyzer;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.Time;


public class ConceptModule implements LinkerModule {
	
	double CONCEPT_WEIGHT = 0.05;
	double CONCEPT_POWER = 0.5;
	double CONCEPT_WIDTH = 5 * 60;
	
	static final int FPS = 25;
	
	Concepts concepts;
	File conceptsDir;
	
	Analyzer analyzer;
	
	public ConceptModule(File conceptsFile,
						 File conceptsDir,
						 Analyzer analyzer) throws IOException {
		concepts = new Concepts(conceptsFile);
		
		this.conceptsDir = conceptsDir;
		this.analyzer = analyzer;
	}
	
	@Override
	public TimelineSet search(Anchor q,
							  TimelineSet currentSet) 
													throws LinkerException {
		try {
			return _search(q, currentSet);
		} catch (Exception e) {
			throw new LinkerException(e);
		}
	}
	
	public TimelineSet _search(Anchor q, TimelineSet currentSet)
															throws Exception {

		List<Concept> conceptObjs = new ArrayList<Concept>();
		
		for (String conceptString : concepts.keySet()) {
			Concept concept = concepts.loadConcept(conceptString, conceptsDir);
			
			if (concept != null) {
				conceptObjs.add(concept);
			}
		}
		
		Iterator<Concept> iter = conceptObjs.iterator();
		
		concepts:
		while (iter.hasNext()) {
			Concept current = iter.next();
			
			Map<Frame, Float> frames = current.findProgrammeFrames(q.fileName);
			
			for (Frame frame : frames.keySet()) {
				float time = frame.frame / 25f;
				
				if (q.startTime <= time && time <= q.endTime) {
					continue concepts;
				}
			}
			
			iter.remove();
		}
		
		TimelineSet timelines = currentSet;
		
		for (Timeline timeline : timelines) {
			for (Concept concept : conceptObjs) {
				Map<Frame, Float> frames =
						concept.findProgrammeFrames(timeline.getID());
				
				float maxConf = 0;
				
				for (Float confidence : frames.values()) {
					maxConf = Math.max(maxConf, confidence);
				}
				
				for (Frame frame : frames.keySet()) {
					
					float time = ((float) frame.frame) / FPS;
					
					// Don't add if past end.
					if (time > timeline.getEndTime() + 30) {
						continue;
					}
					
					ConceptFunction function = 
						new ConceptFunction(CONCEPT_WEIGHT *
												Math.pow(frames.get(frame) /
															maxConf,
														 CONCEPT_POWER),
											time,
											CONCEPT_WIDTH / 3d);
					function.addJustification(
						"Concept '" + concept.toString() + "' matched at " +
					    Time.StoMS(time) + " with confidence " +
						frames.get(frame) /	maxConf);
					
					timeline.addFunction(function);
				}
			}
		}
		
		return timelines;
	}
	
	public class ConceptFunction extends Gaussian
									implements JustifiedTimedFunction {
		List<String> justifications;
		
		double mean;
		
		public ConceptFunction(double norm, double mean, double sigma) {
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
			return "Concept function @ " + Time.StoMS((float) mean);
		}
	}
}
