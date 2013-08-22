package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.openimaj.io.FileUtils;
import org.openimaj.io.IOUtils;

/**
 * Represents the mapping between concept words and their IDs, so as to 
 * facilitate queryingh and loading.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Concepts extends HashMap<String, Integer> {
	
	public Concepts(File conceptsFile) throws IOException {
		super();
		
		String[] lines = FileUtils.readlines(conceptsFile);
		
		for (String line : lines) {
			String[] parts = line.split(" ");
			
			put(parts[1], Integer.parseInt(parts[0]));
		}
	}
	
	public Concept loadConcept(String concept, File conceptsDir) 
														throws IOException {
		Integer conceptID = get(concept);
		
		if (conceptID == null) {
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
