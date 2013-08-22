package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.searchhyper2013.ImageTerrierSearcher.SearchResult;
import org.openimaj.util.pair.ObjectDoublePair;

import ch.qos.logback.core.util.FileUtil;

/**
 * Extends AlphaSearcher by expanding the query using similar frames in a graph 
 * built by LSH over SIFT features.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class DeltaSearcher extends AlphaSearcher {
	public static final int FPS = 25;
	
	public int MAX_EXPANSIONS = 10;
	public int MAX_QUERY_FRAMES = 10;
	public int MAX_FRAME_HITS = 10;
	public float IMAGE_WEIGHT = 0.5f;
	
	Map<String, Map<Integer, String>> shotsDirectoryCache;
	LSHDataExplorer lshExplorer;
	
	public DeltaSearcher(String runName,
						 IndexReader indexReader,
						 File shotsDirectoryCacheFile,
						 LSHDataExplorer lshExplorer) throws IOException {
		super(runName, indexReader);
		
		shotsDirectoryCache = IOUtils.readFromFile(shotsDirectoryCacheFile);
		
		this.lshExplorer = lshExplorer;
	}
	
	ResultList getBaseResults(Query q) throws Exception {
		return super._search(q);
	}
	
	@Override
	ResultList _search(Query q) throws Exception {
		ResultList results = getBaseResults(q);
		
		//System.out.println("\nBase results: \n" + results + "\n");
		
		Map<String, ResultList> imageResults = getImageResults(results, q);
		
		ResultList allResults = chunkResults(imageResults, results);
		
		return allResults;
	}
	
	ResultList chunkResults(Map<String, ResultList> programmeResults, ResultList baseResults) {
		Set<Result> chunked = new TreeSet<Result>();
		
		// Merge within programmes and add to chunked set.
		for (String programme : programmeResults.keySet()) {
			chunked.addAll(programmeResults.get(programme)
							 		   .mergeShortResults(MIN_LENGTH,
							 					 		  MAX_LENGTH));
		}
		
		chunked.addAll(baseResults);
		
		ResultList allResults = new ResultList(baseResults.queryID, baseResults.runName);
		allResults.addAll(chunked);
		
		Collections.sort(allResults);
		
		return allResults;
	}
	
	Map<String, ResultList> getImageResults(ResultList baseResults, Query q)
															throws IOException {
		Map<String, ResultList> imageResults = new HashMap<String, ResultList>();
		
		// Expand on each base result.
		for (int i = 0; i < baseResults.size() && i < MAX_EXPANSIONS; i++) {
			Result result = baseResults.get(i);
			
			List<String> frames = getFrameFilesForResult(result);
			
			// Expand on each frame within this result.
			for (int j = 0; 
				 j < frames.size() && j < MAX_FRAME_HITS;
				 j += (frames.size() / MAX_FRAME_HITS) + 1) {
				String id = frames.get(j);
				
				//System.out.println("\nFrame: " + id + " : " + j);
				
				// Frame may not be in the graph, make sure we handle this.
				List<ObjectDoublePair<String>> rawFrameHits;
				try {
					rawFrameHits = lshExplorer.search(id);
				} catch (IllegalArgumentException e) {
					break;
				}
				
				Map<Frame, Float> frameHits = new HashMap<Frame, Float>();
				
				for (ObjectDoublePair<String> hit : rawFrameHits) {
					frameHits.put(Frame.fromString(hit.getFirst()
													  .replace("/shotdetection/",
															   "")),
								  (float) hit.getSecond());
				}

				Map<String, ResultList> frameResults =
						framesToResults(frameHits,
										q.queryID,
										result.confidenceScore);

				// Merge in results.
				for (String programme : frameResults.keySet()) {
					//System.out.println("\nProgramme frame results: \n" + frameResults.get(programme) + "\n");
					
					ResultList programmeResults = imageResults.get(programme);
					
					if (programmeResults != null) {
						programmeResults.addAll(frameResults.get(programme));
					} else {
						imageResults.put(programme, frameResults.get(programme));
					}
				}
			}
		}
		
		return imageResults;
	}
	private List<String> getFrameFilesForResult(Result result) {
		int firstFrame = ((int) result.startTime) * FPS;
		int lastFrame = (((int) result.endTime) + 1) * FPS;
		
		List<String> framesFiles = new ArrayList<String>();
		
		Map<Integer, String> frames = shotsDirectoryCache.get(result.fileName);
		
		for (Integer frame : frames.keySet()) {
			if (firstFrame <= frame && frame <= lastFrame) {
				String path = "/shotdetection/" + frames.get(frame);
				
				framesFiles.add(path);
			}
		}
		
		return framesFiles;
	}

	Map<String, ResultList> framesToResults(Map<Frame, Float> frameHits, String queryID, float scoreScaleFactor) throws IOException {
		Map<String, ResultList> results = new HashMap<String, ResultList>();
		
		double maxConf = 0;
		
		for (Float score : frameHits.values()) {
			maxConf = Math.max(maxConf, score);
		}
		
		for (Frame frame : frameHits.keySet()) {			
			Result result = new Result();
			
			result.startTime = frame.frame / FPS;
			result.endTime = result.startTime;
			result.jumpInPoint = result.startTime;
			result.fileName = frame.programme;
			
			result.confidenceScore = (float) (frameHits.get(frame) * scoreScaleFactor
											 		   		  * IMAGE_WEIGHT
											 		   		  / maxConf);
			
			ResultList programmeResults = results.get(result.fileName);
			
			if (programmeResults != null) {
				programmeResults.add(result);
			} else {
				programmeResults = new ResultList(queryID, runName);
				programmeResults.add(result);
				results.put(result.fileName, programmeResults);
			}
		}
		
		return results;
	}
	
	static Map<String, Map<Integer, String>> cacheDirectory(File dir) throws IOException {
		final Map<String, Map<Integer, String>> cache = 
				new HashMap<String, Map<Integer, String>>();
		
		Iterator<File> files = FileUtils.iterateFiles(dir,
													  new String[] { "jpg" },
													  true);
		
		while (files.hasNext()) {
			File file = files.next();
			
			String program = file.getParentFile()
								 .getParentFile()
								 .getParentFile()
								 .getName();
			Integer frame = Integer.parseInt(file.getName()
												 .split("\\.")[0]);
			String fileName = file.getAbsolutePath()
								  .replaceAll(".*/(.*?)/shots/(.*)$",
										      "$1/shots/$2");
			
			Map<Integer, String> programFrames = cache.get(program);
			
			if (programFrames == null) {
				programFrames = new HashMap<Integer, String>();
				
				programFrames.put(frame, fileName);
			
				cache.put(program, programFrames);
			} else {
				programFrames.put(frame,  fileName);
			}
		}
		
		return cache;
	}
	
	@Override
	public void configure(Float[] settings) {
		super.configure(settings);
		
		IMAGE_WEIGHT = settings[super.numSettings()];
	}
	
	@Override
	public int numSettings() {
		return super.numSettings() + 1;
	}
	
	/*public static void main(String[] args) throws IOException {
		Map map = cacheDirectory(new File(args[0]));
		IOUtils.writeToFile(map, new File("cache.map"));
	}*/
}
