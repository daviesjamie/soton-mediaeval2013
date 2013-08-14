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
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.searchhyper2013.ImageTerrierSearcher.SearchResult;

/**
 * Extends AlphaSearcher by expanding the query using similar frames in an 
 * ImageTerrier database.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class GammaSearcher extends AlphaSearcher {
	public static final int FPS = 25;
	
	public int MAX_EXPANSIONS = 1000000;
	public int MAX_IMAGETERRIER_HITS = 100;
	public int FRAME_SKIP = 1;
	public float IMAGETERRIER_WEIGHT = 0.2f;
	
	ImageTerrierSearcher imageSearcher;
	File shotsDirectory;
	Map<String, Map<Integer, String>> shotsDirectoryCache;
	
	public GammaSearcher(String runName,
						 IndexReader indexReader,
						 ImageTerrierSearcher imageSearcher,
						 File shotsDirectory) throws IOException {
		super(runName, indexReader);
		
		this.imageSearcher = imageSearcher;
		this.shotsDirectory = shotsDirectory;
		
		shotsDirectoryCache = cacheDirectory(shotsDirectory);
	}
	
	@Override
	ResultList _search(Query q) throws Exception {
		ResultList results = super._search(q);
		
		ResultList imageResults = new ResultList(q.queryID, runName);
		
		for (int i = 0; i < results.size() && i < MAX_EXPANSIONS; i++) {
			Result result = results.get(i);
			
			List<File> frames = getFrameFilesForResult(result);
			
			for (int j = 0; j < frames.size(); j += FRAME_SKIP) {
				File frameFile = frames.get(j);
				
				SearchResult[] frameHits = 
						imageSearcher.search(frameFile,
											 MAX_IMAGETERRIER_HITS);
				
				ResultList frameResults = framesToResults(frameHits,
														  q.queryID,
														  result.confidenceScore);
				System.out.println(frameResults);
				imageResults.addAll(frameResults);
			}
		}
		
		Set<Result> chunked = 
				new HashSet<Result>(
					imageResults.mergeShortResults(MIN_LENGTH, MAX_LENGTH));
		chunked.addAll(results);
		
		ResultList allResults = new ResultList(results.queryID, results.runName);
		allResults.addAll(chunked);
		
		Collections.sort(allResults);
		
		return allResults;
	}
	
	private List<File> getFrameFilesForResult(Result result) {
		int firstFrame = ((int) result.startTime) * FPS;
		int lastFrame = (((int) result.endTime) + 1) * FPS;
		
		List<File> framesFiles = new ArrayList<File>();
		
		Map<Integer, String> frames = shotsDirectoryCache.get(result.fileName);
		
		for (Integer frame : frames.keySet()) {
			if (firstFrame <= frame && frame <= lastFrame) {
				framesFiles.add(new File(frames.get(frame)));
			}
		}
		
		return framesFiles;
	}

	ResultList framesToResults(SearchResult[] frameHits, String queryID, float scoreScaleFactor) {
		ResultList results = new ResultList(queryID, runName);
		
		for (SearchResult hit : frameHits) {
			int frame =	Integer.parseInt(Paths.get(hit.fileName)
											  .getFileName()
											  .toString()
											  .split("\\.")[0]);
			
			Result result = new Result();
			
			result.startTime = frame / FPS;
			result.endTime = result.startTime;
			result.jumpInPoint = result.startTime;
			result.fileName = Paths.get(hit.fileName)
								   .getParent()
								   .getParent()
								   .getParent()
								   .getFileName()
								   .toString();
			
			result.confidenceScore = (float) hit.score * scoreScaleFactor
													   * IMAGETERRIER_WEIGHT;
			
			results.add(result);
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
			
			Map<Integer, String> programFrames = cache.get(program);
			
			if (programFrames == null) {
				programFrames = new HashMap<Integer, String>();
				
				programFrames.put(frame, file.getAbsolutePath());
			
				cache.put(program, programFrames);
			} else {
				programFrames.put(frame,  file.getAbsolutePath());
			}
		}
		
		return cache;
	}
	
	public static void main(String[] args) throws IOException {
		Map map = cacheDirectory(new File(args[0]));
		IOUtils.writeToFile(map, new File("cache.map"));
	}
}
