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

import org.openimaj.io.IOUtils;

/**
 * Extends an ArrayList<Result> to provide additional functionality, such as 
 * writing out the results to the submission format.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class ResultList extends ArrayList<Result> {
	private static final float MIN_RESULT_LENGTH = 60 * 1;
	
	private String queryID;
	private String runName;

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
			
			stringBuilder.append(queryID                + " " + 
								 "Q0"                   + " " +
								 result.fileName        + " " + 
								 result.startTime       + " " +
								 result.endTime         + " " +
								 result.jumpInPoint     + " " + 
								 (i + 1)                + " " +
								 result.confidenceScore + " " + 
								 runName				+ "\n");
		}
		
		return stringBuilder.toString();
	}
	
	public static ResultList fromHighlightedTranscripts(String queryID,
														String runName, 
														String fileName, 
														Collection<? extends HighlightedTranscript> transcripts) {
		ResultList results = new ResultList(queryID, runName);
		
		float avgConf = 0f;
		
		for (HighlightedTranscript transcript : transcripts) {
			avgConf += transcript.confidence();
		}
		
		avgConf /= transcripts.size();
		
		for (HighlightedTranscript transcript : transcripts) {
			if (transcript.length() < MIN_RESULT_LENGTH) {
				continue;
			}
			
			Result result = new Result();
			
			result.startTime = transcript.startTime();
			result.endTime = transcript.endTime();
			result.jumpInPoint = transcript.startTime();
			result.confidenceScore = transcript.confidence() * avgConf;
			result.fileName = fileName;
			
			results.add(result);
		}
		
		return results;
	}
}
