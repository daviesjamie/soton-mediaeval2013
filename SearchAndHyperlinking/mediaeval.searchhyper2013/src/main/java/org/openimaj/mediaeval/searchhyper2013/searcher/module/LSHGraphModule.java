package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.searchhyper2013.datastructures.JustifiedTimedFunction;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.openimaj.util.pair.ObjectDoublePair;

public class LSHGraphModule implements SearcherModule {

	double MIN_INTEGRAL = 1e-3;
	double LSH_WEIGHT = 0.5;
	double LSH_POWER = 0.5;
	double LSH_WIDTH = 60;
	
	Map<String, Map<Integer, String>> shotsDirCache;
	LSHDataExplorer lshGraph;
	TimelineFactory timelineFactory;
	
	public LSHGraphModule(File shotsDirCacheFile,
						  LSHDataExplorer lshGraph,
						  TimelineFactory timelineFactory) throws IOException {
		shotsDirCache = IOUtils.readFromFile(shotsDirCacheFile);
		
		this.lshGraph = lshGraph;
		this.timelineFactory = timelineFactory;
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
	
	public TimelineSet _search(Query q, TimelineSet currentSet) throws IOException {
		
		final int FPS = 25;
		
		UnivariateIntegrator integrator = 
				new TrapezoidIntegrator(1e-3, 1e-3, 2, 64);
		
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (Timeline timeline : currentSet) {
			SortedMap<Integer, String> frames =
					new TreeMap<Integer, String>(
							shotsDirCache.get(timeline.getID()));
			
			float[] boundaries = timeline.getShotBoundaries();
			
			double[] integrals = new double[boundaries.length];
			
			for (int i = 0; i < boundaries.length; i++) {
				float start = i == 0 ? 0 : boundaries[i - 1];
				float end = boundaries[i];
				
				integrals[i] = integrator.integrate(10000,
													timeline,
													start,
													end);
			}
			
			for (int i = 0; i < integrals.length; i++) {
				if (integrals[i] > MIN_INTEGRAL) {
					int upperFrame = (int) boundaries[i] * FPS;
					
					SortedMap<Integer, String> lteq = 
						new TreeMap<Integer, String>(frames.headMap(upperFrame));
					lteq.put(upperFrame, frames.get(upperFrame));
					
					String frame = "/shotdetection/" + 
								   frames.get(lteq.lastKey());
					
					List<ObjectDoublePair<String>> results;
					
					// Fail gracefully if a frame is not in the graph.
					try {
						results = lshGraph.search(frame);
					} catch (IllegalArgumentException e) {
						continue;
					}
					
					// Normalise.
					double maxConf = 0;
					
					for (ObjectDoublePair<String> result : results) {
						maxConf = Math.max(maxConf,  result.getSecond());
					}
					
					for (ObjectDoublePair<String> result : results) {
						result.setSecond(result.getSecond() / maxConf);
						
						String programme =
								result.getFirst()
									  .replace("/shotdetection/", "")
									  .split("/")[0];
						
						if (programme.equals(timeline.getID())) {
							continue;
						}
						
						float time =
								Float.parseFloat(
									result.getFirst()
										  .replace("/shotdetection/", "")
										  .split("/")[3]
										  .split("\\.")[0]) / FPS;
						
						Timeline tl = timeline;
						
						if (!timeline.getID().equals(programme)) {
							tl = timelineFactory.makeTimeline(programme);
						}
						
						LSHGraphFunction function = 
							new LSHGraphFunction(LSH_WEIGHT * integrals[i] *
							 			 			Math.pow(result.getSecond(),
							 			 					 LSH_POWER),
							 			 		 time,
							 			 		 LSH_WIDTH / 3d);
						
						function.addJustification(
								"LSH match found between " + timeline.getID() +
								" (" +
								Time.StoMS(i == 0 ? 0 : boundaries[i - 1]) +
								" - " + Time.StoMS(boundaries[i]) + ") and " +
								Time.StoMS(time) + " with integral " + 
								integrals[i] + " and graph weight " + 
								result.getSecond());
						
						tl.addFunction(function);
						timelines.add(tl);
					}
				}
			}
		}
		
		return timelines;
	}

	public class LSHGraphFunction extends Gaussian
									implements JustifiedTimedFunction {
		List<String> justifications;
		
		double mean;
		
		public LSHGraphFunction(double norm, double mean, double sigma) {
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
			return "LSHGraph function @ " + Time.StoMS((float) mean);
		}
	}
}
