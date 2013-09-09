package org.openimaj.mediaeval.searchhyper2013.linker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ContextedAnchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Frame;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.openimaj.util.pair.ObjectDoublePair;

public class AlphaLinker implements Linker {
	
	static final int FPS = 25;
	
	public float MIN_LENGTH = 1 * 60;
	public float MAX_LENGTH = 10 * 60;
	public float IMAGE_POWER = 2f;
	
	String runName;
	Map<String, Map<Integer, String>> shotsDirectoryCache;
	LSHDataExplorer lshExplorer;

	public AlphaLinker(String runName,
					   File shotsDirectoryCacheFile,
					   LSHDataExplorer lshExplorer) throws IOException {
		
		this.runName = runName;
		
		shotsDirectoryCache = IOUtils.readFromFile(shotsDirectoryCacheFile);
		
		this.lshExplorer = lshExplorer;
	}
	
	@Override
	public ResultList link(Anchor q) throws LinkerException {
		try {
			return _link(q);
		} catch (Exception e) {
			throw new LinkerException(e);
		}
	}
	
	private ResultList _link(Anchor q) throws IOException {
		ResultList results = new ResultList(q.anchorID, runName);
		
		Result anchorResult = new Result();
		
		anchorResult.confidenceScore = 1f;
		anchorResult.fileName = q.fileName;
		
		if (q instanceof ContextedAnchor) {
			anchorResult.startTime = ((ContextedAnchor) q).contextStartTime;
			anchorResult.endTime = ((ContextedAnchor) q).contextEndTime;
		} else {
			anchorResult.startTime = q.startTime;
			anchorResult.endTime = q.endTime;
		}
		
		anchorResult.jumpInPoint = anchorResult.startTime;
		
		List<String> frames = getFrameFilesForResult(anchorResult);
		
		Map<String, ResultList> resultsByProgramme = new HashMap<String, ResultList>();
		
		for (String frame : frames) {
			List<ObjectDoublePair<String>> expansionFrames;
			
			try {
				expansionFrames = lshExplorer.expandedSearch(frame, true);
			} catch (IllegalArgumentException e) {
				break;
			}
			
			double maxScore = 0;
			
			for (ObjectDoublePair<String> expansionFrame : expansionFrames) {
				maxScore = Math.max(expansionFrame.second, maxScore);
			}
			
			Map<Frame, Float> frameHits = new HashMap<Frame, Float>();
			
			for (ObjectDoublePair<String> expansionFrame : expansionFrames) {
				expansionFrame.second /= maxScore;
				
				frameHits.put(Frame.fromString(expansionFrame.getFirst()
						  									 .replace("/shotdetection/",
						  											  "")),
						  	  (float) expansionFrame.getSecond());
			}
			
			Map<String, ResultList> frameResults = framesToResults(frameHits,
																   q.anchorID);

			// Merge in results.
			for (String programme : frameResults.keySet()) {
				//System.out.println("\nProgramme frame results: \n" + frameResults.get(programme) + "\n");
				
				ResultList programmeResults = resultsByProgramme.get(programme);
				
				if (programmeResults != null) {
					programmeResults.addAll(frameResults.get(programme));
				} else {
					resultsByProgramme.put(programme, frameResults.get(programme));
				}
			}
		}
		
		Set<Result> resultSet = new TreeSet<Result>();
		
		// Merge within programmes and add to set.
		for (String programme : resultsByProgramme.keySet()) {
			resultSet.addAll(resultsByProgramme.get(programme)
							 		   	   	   .mergeShortResults(MIN_LENGTH,
							 		   			   			  	  MAX_LENGTH));
		}
		
		resultSet.add(anchorResult);
		
		results.addAll(resultSet);
		
		Collections.sort(results);
		
		return results;
	}

	Map<String, ResultList> framesToResults(Map<Frame, Float> frameHits, String queryID) throws IOException {
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
			
			result.confidenceScore = 
					(float) Math.pow(frameHits.get(frame) / maxConf, IMAGE_POWER);
			
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
	
	@Override
	public void configure(Float[] settings) {
		IMAGE_POWER = settings[0];
	}

	@Override
	public int numSettings() {
		return 1;
	}

}
