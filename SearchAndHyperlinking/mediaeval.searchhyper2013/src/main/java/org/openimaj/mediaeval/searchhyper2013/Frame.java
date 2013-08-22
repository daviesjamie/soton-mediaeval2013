package org.openimaj.mediaeval.searchhyper2013;

/**
 * Represents a single frame in a programme.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Frame {
	int frame;
	String programme;
	
	/**
	 * This method assumes an input of the form 
	 * "<programme>/<shot>/<frame>.jpg"
	 * 
	 * @param string
	 * @return
	 */
	public static Frame fromString(String string) {
		Frame frame = new Frame();
		
		String[] components = string.split("/");
		
		frame.programme = components[0];
		frame.frame = Integer.parseInt(components[2].split("\\.")[0]);
		
		return frame;
	}
}
