package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a concept file.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Concept extends HashMap<Frame, Float> {
	private static final long serialVersionUID = 599335348853875725L;
	
	String concept;
	
	public Concept(String concept) {
		super();
		
		this.concept = concept;
	}

	/**
	 * Searches through the Map for Frames that match the given programme name.
	 * 
	 * @param programme
	 * @return Matching sub-Map.
	 */
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
