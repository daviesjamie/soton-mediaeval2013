package org.openimaj.mediaeval.searchhyper2013;

import java.util.List;

/**
 * A Searcher object implements a specific search algorithm for the purpose of 
 * fulfilling the search task.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public interface Searcher {
	
	public ResultList search(Query q) throws SearcherException;

	public void configure(Float[] settings);

	int numSettings();
}
