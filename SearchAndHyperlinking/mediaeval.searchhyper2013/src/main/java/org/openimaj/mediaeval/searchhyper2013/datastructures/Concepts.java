package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.openimaj.io.FileUtils;

/**
 * Represents the mapping between concept words and their IDs, so as to 
 * facilitate querying and loading.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Concepts extends HashMap<String, Integer> {
	private static final long serialVersionUID = -4162740867199679860L;

	public Concepts(File conceptsFile) throws IOException {
		super();
		
		String[] lines = FileUtils.readlines(conceptsFile);
		
		for (String line : lines) {
			String[] parts = line.split(" ");
			
			put(parts[1], Integer.parseInt(parts[0]));
		}
	}
	
	/**
	 * Loads a Concept by name by looking up its ID to determine the filename.
	 * 
	 * @param concept
	 * @param conceptsDir
	 * @return The Concept object.
	 * @throws IOException
	 */
	public Concept loadConcept(String concept, File conceptsDir) 
														throws IOException {
		Integer conceptID = get(concept);
		
		final Integer[] deadConcepts = { 702, 977, 455, 934, 222, 548 };
		Arrays.sort(deadConcepts);
		
		if (conceptID == null || Arrays.binarySearch(deadConcepts, conceptID) > -1) {
			return null;
		}
		
		File conceptFile = new File(conceptsDir.getAbsolutePath() + 
									"/me13sh-" + conceptID + ".trec");
		
		Concept conceptObj = new Concept(concept);
		
		String[] lines = FileUtils.readlines(conceptFile);
		
		for (String line : lines) {
			String[] parts = line.split(" ");
			
			Frame frame =
				Frame.fromString(parts[1].replace("keyframes/cBBC.sh13/v", ""));
			
			conceptObj.put(frame, Float.parseFloat(parts[2]));
		}
		
		return conceptObj;
	}
	
	public String conceptsString() {
		StringBuilder sb = new StringBuilder();
		
		for (String concept : keySet()) {
			sb.append(concept + " ");
		}
		
		return sb.toString();
	}
}
