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

import ch.qos.logback.core.util.FileUtil;

/**
 * Extends AlphaSearcher by expanding the query using similar frames in an 
 * ImageTerrier database.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class GammaSearcher extends AlphaSearcher {
	public static final int FPS = 25;
	
	public int MAX_EXPANSIONS = 10;
	public int MAX_QUERY_FRAMES = 10;
	public int MAX_FRAME_HITS = 10;
	public float IMAGE_WEIGHT = 0.8f;
	
	IndexReader imageIndexReader;
	Map<String, Map<Integer, String>> shotsDirectoryCache;
	
	public GammaSearcher(String runName,
						 IndexReader indexReader,
						 IndexReader imageIndexReader,
						 File shotsDirectoryCacheFile) throws IOException {
		super(runName, indexReader);
		
		this.imageIndexReader = imageIndexReader;
		
		System.out.print("Reading cache file... ");
		shotsDirectoryCache = IOUtils.readFromFile(shotsDirectoryCacheFile);
		System.out.println("Done!");
	}
	
	@Override
	ResultList _search(Query q) throws Exception {
		// Get base results.
		ResultList results = super._search(q);
		
		System.out.println("\nBase results: \n" + results + "\n--");
		
		Map<String, ResultList> imageResults = new HashMap<String, ResultList>();
		
		IndexSearcher imageIndexSearcher = new IndexSearcher(imageIndexReader);
		
		StandardQueryParser queryParser =
				new StandardQueryParser(new WhitespaceAnalyzer(LUCENE_VERSION));
		BooleanQuery.setMaxClauseCount(1000000);
		
		// Expand on each base result.
		for (int i = 0; i < results.size() && i < MAX_EXPANSIONS; i++) {
			Result result = results.get(i);
			
			List<String> frames = getFrameFilesForResult(result);
			
			// Expand on each frame within this result.
			for (int j = 0; 
				 j < frames.size() && j < MAX_FRAME_HITS;
				 j += (frames.size() / MAX_FRAME_HITS) + 1) {
				String id = frames.get(j);
				
				System.out.println("\nFrame: " + id + " : " + j);
				
				Document frameDoc =
						imageIndexReader.document(
							imageIndexSearcher.search(
								new TermQuery(new Term("id", id)),
								1)
							.scoreDocs[0]
							.doc);
				
				org.apache.lucene.search.Query query = 
						queryParser.parse(frameDoc.get("qsift"), "qsift");
				
				ScoreDoc[] hits = 
						imageIndexSearcher.search(
							query,
							MAX_FRAME_HITS)
						.scoreDocs;
				
				Map<String, ResultList> frameResults =
						framesToResults(hits,
										q.queryID,
										result.confidenceScore);

				// Merge in results.
				for (String programme : frameResults.keySet()) {
					System.out.println("\nProgramme frame results: \n" + frameResults.get(programme) + "\n--");
					
					ResultList programmeResults = imageResults.get(programme);
					
					if (programmeResults != null) {
						programmeResults.addAll(frameResults.get(programme));
					} else {
						imageResults.put(programme, frameResults.get(programme));
					}
				}
			}
		}
		
		Set<Result> chunked = new HashSet<Result>();
		
		// Merge within programmes and add to chunked set.
		for (String programme : imageResults.keySet()) {
			chunked.addAll(imageResults.get(programme)
							 		   .mergeShortResults(MIN_LENGTH,
							 					 		  MAX_LENGTH));
		}
		
		chunked.addAll(results);
		
		ResultList allResults = new ResultList(results.queryID, results.runName);
		allResults.addAll(chunked);
		
		Collections.sort(allResults);
		
		return allResults;
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

	Map<String, ResultList> framesToResults(ScoreDoc[] frameHits, String queryID, float scoreScaleFactor) throws IOException {
		Map<String, ResultList> results = new HashMap<String, ResultList>();
		
		for (ScoreDoc hit : frameHits) {
			Document frameDoc = imageIndexReader.document(hit.doc);
			
			String[] fileNameParts = frameDoc.get("id").split("/");
			
			int frame = Integer.parseInt(fileNameParts[5].split("\\.")[0]);
			
			Result result = new Result();
			
			result.startTime = frame / FPS;
			result.endTime = result.startTime;
			result.jumpInPoint = result.startTime;
			result.fileName = fileNameParts[2];
			
			result.confidenceScore = (float) hit.score * scoreScaleFactor
													   * IMAGE_WEIGHT;
			
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
	
	/*public static void main(String[] args) throws IOException {
		Map map = cacheDirectory(new File(args[0]));
		IOUtils.writeToFile(map, new File("cache.map"));
	}*/
}
