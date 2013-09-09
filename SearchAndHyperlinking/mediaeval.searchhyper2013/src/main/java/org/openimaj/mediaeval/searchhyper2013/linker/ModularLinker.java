package org.openimaj.mediaeval.searchhyper2013.linker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.AnchorList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultSet;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.linker.module.ConceptModule;
import org.openimaj.mediaeval.searchhyper2013.linker.module.LSHGraphModule;
import org.openimaj.mediaeval.searchhyper2013.linker.module.LinkerModule;
import org.openimaj.mediaeval.searchhyper2013.linker.module.SearcherModuleWrapperModule;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.ModularSearcher;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherEvaluator;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.ChannelFilterModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SearcherModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SynopsisModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.TitleModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.TranscriptModule;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;

public class ModularLinker implements Linker {

	float MERGE_WINDOW = 1 * 60;
	int MAX_RESULTS_PER_TIMELINE = 10;

	String runName;
	
	//StandardQueryParser queryParser;
	List<LinkerModule> linkerModules;
	
	public ModularLinker(String runName) {
		this.runName = runName;
		
		linkerModules = new ArrayList<LinkerModule>();
	}

	@Override
	public ResultList link(Anchor q) throws LinkerException {
		// Accumulated timelines.
		TimelineSet timelines = new TimelineSet();
		
		for (LinkerModule module : linkerModules) {
			timelines = module.search(q, timelines);
		}
		
		//System.out.println("No. timelines: " + timelines.size());
		
		//ResultList results = kMeans(q, timelines);
		ResultList results;
		try {
			results = shotBoundaries(q, timelines);
		} catch (Exception e) {
			throw new LinkerException(e);
		}
		
		Collections.sort(results);
		
		/*List<String> programmesToPrint = new ArrayList<String>();
		for (int i = 0; i < results.size() && i < 3; i++) {
			programmesToPrint.add(results.get(i).fileName);
		}
		
		for (Timeline timeline : timelines) {
			if (programmesToPrint.contains(timeline.getID()) ||
					expectedFile.equals(timeline.getID())) {
				System.out.println(timeline);
				
				for (String j : timeline.getJustifications()) {
					System.out.println("\t" + j);
				}
				
				List<JustifiedTimedFunction> fs =
					new ArrayList<JustifiedTimedFunction>(timeline.getFunctions());
				Collections.sort(fs, new JustifiedTimedFunction.TimeComparator());
				
				for (JustifiedTimedFunction f : fs) {
					System.out.println("\t" + f.toString());
					
					for (String j : f.getJustifications()) {
						System.out.println("\t\t" + j);
					}
				}
				
				System.out.println("--");
				
				if (plot) timeline.plot();
			}
		}*/
		
		return results;
	}

	private ResultList shotBoundaries(Anchor q, TimelineSet timelines) throws Exception {
		UnivariateIntegrator integrator = 
				new TrapezoidIntegrator(1e-3, 1e-3, 2, 64);
		
		ResultSet resultSet = new ResultSet(MERGE_WINDOW);
		
		for (Timeline timeline : timelines) {			
			float[] shotBoundaries = timeline.getShotBoundaries();
			
			double[] integrals = new double[shotBoundaries.length];
			
			for (int i = 0; i < shotBoundaries.length; i++) {
				float start = i == 0 ? 0 : shotBoundaries[i - 1];
				float end = shotBoundaries[i];
				
				integrals[i] = integrator.integrate(1000000,
													timeline,
													start,
													end);
			}
			
			double[] bestIntegrals = Arrays.copyOf(integrals, integrals.length);
			Arrays.sort(bestIntegrals);
			bestIntegrals =
					Arrays.copyOfRange(bestIntegrals,
								   	   Math.max(bestIntegrals.length -
								   			   		MAX_RESULTS_PER_TIMELINE,
								   			   	0),
								   	   bestIntegrals.length);
			
			for (double integral : bestIntegrals) {
				int startIndex = -2;
				
				for (int i = 0; i < integrals.length; i++) {
					if (integrals[i] == integral) {
						startIndex = i - 1;
						
						break;
					}
				}
				
				if (startIndex == -2) {
					throw new Exception("OSHIT");
				}
				
				int minBoundaryIndex = startIndex;// - SHOTS_WIDTH;
				int maxBoundaryIndex = startIndex + 1;// + SHOTS_WIDTH;
				
				float start = minBoundaryIndex < 0 ?
								0 : shotBoundaries[minBoundaryIndex];
				float jumpIn = startIndex < 0 ? 0 : shotBoundaries[startIndex];
				float end = maxBoundaryIndex > shotBoundaries.length - 1 ?
								timeline.getEndTime() : 
								shotBoundaries[maxBoundaryIndex];
								
				double score = integrator.integrate(10000,
													timeline,
													start,
													end);
				
				Result result = new Result();
				
				result.fileName = timeline.getID();
				result.startTime = start;
				result.jumpInPoint = -1;
				result.endTime = end;
				result.confidenceScore = score;
				
				resultSet.add(result);
			}
		}
		
		// Normalise.
		double maxScore = 0;
		
		for (Result result : resultSet) {
			maxScore = Math.max(result.confidenceScore, maxScore);
		}
		
		for (Result result : resultSet) {
			result.confidenceScore /= maxScore;
		}
		
		ResultList results = new ResultList(q.anchorID, runName);
		
		results.addAll(resultSet);
		
		return results;
	}
	
	@Override
	public void configure(Float[] settings) {
		// TODO Auto-generated method stub

	}

	@Override
	public int numSettings() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean addModule(LinkerModule module) {
		return linkerModules.add(module);
	}
	
	public static void main(String[] args) throws Exception {
		File anchorsFile = new File(args[0]);
		
		Directory luceneDir = FSDirectory.open(new File(args[1]));
		IndexReader indexReader = DirectoryReader.open(luceneDir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		EnglishAnalyzer englishAnalyzer =
				new EnglishAnalyzer(Version.LUCENE_43);
		
		TimelineFactory timelineFactory = 
				new TimelineFactory(indexSearcher, new File(args[2]));
		
		Directory spellDir = FSDirectory.open(new File(args[3]));
		
		//LSHDataExplorer lshGraph = new LSHDataExplorer(new File(args[4]), 20);
		
		ChannelFilterModule channelFilterModule = new ChannelFilterModule();
		SynopsisModule synopsisModule = new SynopsisModule(indexSearcher,
														   spellDir,
														   timelineFactory);
		TranscriptModule transcriptModule =
				new TranscriptModule(indexSearcher,
									 Type.Subtitles,
									 englishAnalyzer,
									 spellDir,
									 timelineFactory);
		TitleModule titleModule = new TitleModule(indexSearcher,
				  spellDir,
				  timelineFactory);
		ConceptModule conceptModule = new ConceptModule(new File(args[5]),
														new File(args[6]),
														englishAnalyzer);
		//LSHGraphModule lshGraphModule = new LSHGraphModule(new File(args[7]),
		//												   lshGraph,
		//												   timelineFactory);
		
		ModularLinker searcher =
				new ModularLinker("me13sh_soton-wais2013_LA_Sh_S_MV_ModularConcepts");
		searcher.addModule(new SearcherModuleWrapperModule(synopsisModule, indexSearcher));
		searcher.addModule(new SearcherModuleWrapperModule(transcriptModule, indexSearcher));
		searcher.addModule(new SearcherModuleWrapperModule(titleModule, indexSearcher));
		searcher.addModule(new SearcherModuleWrapperModule(channelFilterModule, indexSearcher));
		//searcher.addModule(conceptModule);
		//searcher.addModule(lshGraphModule);
		
		// Filter for synopsis- and title-only hits.
		searcher.addModule(new LinkerModule() {

			@Override
			public TimelineSet search(Anchor q, TimelineSet currentSet)
					throws LinkerException {
				TimelineSet timelines = new TimelineSet();
				
				for (Timeline timeline : currentSet) {
					if (timeline.numFunctions() > 0) {
						timelines.add(timeline);
					}
				}
				
				return timelines;
			}
			
		});
		
		AnchorList anchors = AnchorList.readFromFile(anchorsFile, false);

		for (Anchor q : anchors) {
			
			if (args.length <= 8 || (args.length > 8 && q.anchorID.equals(args[8])))
				System.out.println(searcher.link(q));
		}

	}
}
