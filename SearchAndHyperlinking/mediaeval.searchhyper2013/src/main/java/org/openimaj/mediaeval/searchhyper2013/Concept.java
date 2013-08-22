package org.openimaj.mediaeval.searchhyper2013;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a concept file.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Concept extends HashMap<Frame, Float> {
	String concept;
	
	public Concept(String concept) {
		super();
		
		this.concept = concept;
	}

	public Map<Frame, Float> findProgrammeFrames(String programme) {
		Map<Frame, Float> matches = new HashMap<Frame, Float>();
		
		for (Map.Entry<Frame, Float> entry : this.entrySet()) {
			if (entry.getKey().programme.equals(programme)) {
				matches.put(entry.getKey(), entry.getValue());
			}
		}
		
		return matches;
	}
}
