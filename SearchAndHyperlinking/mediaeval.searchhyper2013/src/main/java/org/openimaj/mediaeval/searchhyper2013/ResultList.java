package org.openimaj.mediaeval.searchhyper2013;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openimaj.io.IOUtils;
import org.openimaj.util.pair.Pair;

/**
 * Extends an ArrayList<Result> to provide additional functionality, such as 
 * writing out the results to the submission format.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class ResultList extends ArrayList<Result> {
	String queryID;
	String runName;

	public ResultList(String queryID, String runName) {
		this.queryID = queryID;
		this.runName = runName;
	}
	
	public void writeToFile(File outFile) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		
		out.write(toString());
		
		out.close();
	}
	
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		
		for (int i = 0; i < size(); i++) {
			Result result = get(i);
			
			stringBuilder.append(queryID                		+ " " + 
								 "Q0"                   		+ " " +
								 result.fileName        		+ " " + 
								 Time.StoMS(result.startTime)   + " " +
								 Time.StoMS(result.endTime)     + " " +
								 Time.StoMS(result.jumpInPoint) + " " + 
								 (i + 1)                		+ " " +
								 result.confidenceScore 		+ " " + 
								 runName						+ "\n");
		}
		
		return stringBuilder.toString();
	}
	
	public static ResultList fromHighlightedTranscripts(String queryID,
														String runName, 
														String fileName, 
														List<HighlightedTranscript> transcripts,
														float confidenceScale,
														float minLength,
														float maxLength) {
		List<HighlightedTranscript> splitTrans =
				splitLongTranscripts(transcripts, minLength, maxLength);
		
		ResultList results = new ResultList(queryID, runName);
		
		float totalConf = 0f;
		
		for (HighlightedTranscript transcript : splitTrans) {
			totalConf += transcript.confidence();
		}
		
		for (HighlightedTranscript transcript : splitTrans) {
			Result result = new Result();
			
			result.startTime = transcript.startTime();
			result.endTime = transcript.endTime();
			result.jumpInPoint = transcript.startTime();
			result.confidenceScore = confidenceScale * (transcript.confidence() / totalConf);
			result.fileName = fileName;
			
			results.add(result);
		}

		return results.mergeShortResults(minLength, maxLength);
	}
	
	private static List<HighlightedTranscript> splitLongTranscripts
					(List<HighlightedTranscript> transcripts,
					 float minLength,
					 float maxLength) {
		
		List<HighlightedTranscript> splitTranscripts =
				new ArrayList<HighlightedTranscript>();
		
		for (HighlightedTranscript transcript : transcripts) {
			for (HighlightedTranscript splitTrans : transcript.splitByMaxLength(maxLength)) {
				if (splitTrans.length() < maxLength) {
					splitTranscripts.add(splitTrans);
				}
			}
		}
		
		return splitTranscripts;
	}
	
	public ResultList mergeShortResults(float minLength, float maxLength) {
		Result[] array = toArray(new Result[0]);
		
		Pair<Result> nearest = null;
		float nearestDist = Float.MAX_VALUE;
		
		for (int i = 0; i < array.length - 1; i++) {
			Result a = array[i];
			
			if (a.length() > minLength) {
				continue;
			}
			
			for (int j = i + 1; j < array.length; j++) {
				Result b = array[j];
				
				if (b.length() > minLength) {
					continue;
				}
				
				float dist = (float) Math.sqrt(Math.pow(a.startTime - b.startTime, 2) +
									   		   Math.pow(a.endTime - b.endTime, 2));
				
				if (dist < nearestDist) {
					nearestDist = dist;
					
					nearest = new Pair<Result>(a, b);
				}
			}
		}
		
		if (nearest != null) {
			Result mergedResult = new Result();
			mergedResult.startTime = nearest.firstObject().startTime;
			mergedResult.endTime = nearest.secondObject().endTime;
			mergedResult.jumpInPoint = nearest.firstObject().jumpInPoint;
			mergedResult.fileName = nearest.firstObject().fileName;
			mergedResult.confidenceScore = nearest.firstObject().confidenceScore +
										   nearest.secondObject().confidenceScore;
			
			if (mergedResult.length() > maxLength) {
				return this;
			}
		
			ResultList merged = new ResultList(queryID, runName);
			
			merged.add(mergedResult);
			
			for (Result result : this) {
				if (!result.equals(nearest.firstObject()) &&
					!result.equals(nearest.secondObject())) {
						merged.add(result);
				}
			}
			
			return merged.mergeShortResults(minLength, maxLength);
		} else {
			return this;
		}
	}

}
