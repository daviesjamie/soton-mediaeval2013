package org.openimaj.mediaeval.searchhyper2013.searcher;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenGroup;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Concept;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Concepts;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Frame;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.lucene.EnglishSynonymAnalyzer;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.openimaj.mediaeval.searchhyper2013.util.Maps;

/**
 * Extends DeltaSearcher by including additional concept matching frames.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class EpsilonSearcher extends DeltaSearcher {
	
	public float CONCEPT_FRAME_POWER = 2f;
	public float CONCEPT_FRAME_WEIGHT = 10f;
	
	Concepts concepts;
	File conceptsDir;
	File synFile;
	
	public EpsilonSearcher(String runName,
						 IndexReader indexReader,
						 File shotsDirectoryCacheFile,
						 LSHDataExplorer lshExplorer,
						 File conceptsDir,
						 File conceptsFile,
						 File synFile) throws IOException {
		super(runName, indexReader, shotsDirectoryCacheFile, lshExplorer);
		
		concepts = new Concepts(conceptsFile);
		this.conceptsDir = conceptsDir;
		this.synFile = synFile;
	}
	
	@Override
	ResultList _search(Query q) throws Exception {
		//System.out.println(q);
		
		ResultList baseResults = getBaseResults(q);
		
		Map<String, ResultList> imageResults = getImageResults(baseResults, q);
		
		List<String> queryConcepts =
				getCommonTokens(q.queryText + " " + q.visualCues,
								this.concepts.conceptsString());
		
		//System.out.println(queryConcepts);
		
		List<Concept> conceptObjs = new ArrayList<Concept>();
		
		for (String concept : queryConcepts) {
			conceptObjs.add(concepts.loadConcept(concept, conceptsDir));
		}
		
		for (String programme : imageResults.keySet()) {
			for (Concept concept : conceptObjs) {
				Map<Frame, Float> frames = concept.findProgrammeFrames(programme);
				
				//System.out.println("Concept frames: \n" + frames + "\n");
				
				// Normalise
				float maxConf = 0;
				
				for (Float conf : frames.values()) {
					maxConf = Math.max(maxConf, conf);
				}
				
				for (Frame frame : frames.keySet()) {
					frames.put(frame, (float) Math.pow(frames.get(frame) / maxConf, CONCEPT_FRAME_POWER));
				}
				
				Map<String, ResultList> results = framesToResults(frames,
																  q.queryID,
																  CONCEPT_FRAME_WEIGHT);
				//System.out.println(results);
				Maps.mergeMap(imageResults, results);
			}
		}
		
		return chunkResults(imageResults, baseResults);
	}
	
	public List<String> getCommonTokens(String queryString, String searchString) throws IOException, QueryNodeException, InvalidTokenOffsetsException {
		
		Map<String, String> analyzerArgs = new HashMap<String, String>();
		
		analyzerArgs.put("luceneMatchVersion", LUCENE_VERSION.toString());
		analyzerArgs.put("synonyms", synFile.getAbsolutePath());
		analyzerArgs.put("format", "wordnet");
		analyzerArgs.put("ignoreCase", "true");
		analyzerArgs.put("expand", "true");
		
		EnglishSynonymAnalyzer englishSynonymAnalyzer =
				new EnglishSynonymAnalyzer(LUCENE_VERSION, analyzerArgs);
		
		StandardQueryParser queryParser =
				new StandardQueryParser(englishSynonymAnalyzer);
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
				new EnglishAnalyzer(LUCENE_VERSION)
							.tokenStream("foo",
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
	
	@Override
	public void configure(Float[] settings) {
		super.configure(settings);
		
		CONCEPT_FRAME_POWER = settings[super.numSettings()];
		CONCEPT_FRAME_WEIGHT = settings[super.numSettings() + 1];
	}
	
	@Override
	public int numSettings() {
		return super.numSettings() + 2;
	}
}
