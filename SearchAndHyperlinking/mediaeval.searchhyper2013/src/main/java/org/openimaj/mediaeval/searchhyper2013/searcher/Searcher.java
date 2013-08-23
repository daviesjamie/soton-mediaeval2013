package org.openimaj.mediaeval.searchhyper2013.searcher;

import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;

/**
 * A Searcher object implements a specific search algorithm for the purpose of 
 * fulfilling the search task.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public interface Searcher {
	
	public ResultList search(Query q) throws SearcherException;

	/**
	 * Provides Searchers with the option for arbitrary external configuration 
	 * in a manner compatible with SearcherEvaluator.
	 * @param settings
	 */
	public void configure(Float[] settings);

	/**
	 * Returns the number of settings configured by this Searcher, so as to 
	 * permit simple and de-coupled coding of configure() down the inheritance 
	 * hierarchy.
	 * 
	 * @return
	 */
	int numSettings();
}
