package org.openimaj.mediaeval.searchhyper2013;

import gov.sandia.cognition.math.matrix.Vector;

import java.util.List;

/**
 * Represents classes that can search queries.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public interface Searcher {
	public List<Result> search(Query q);

	/**
	 * A Searcher may allow certain properties to be set by a vector so that it 
	 * can be optimised with the OptimiseSearcher program.
	 * @param input
	 */
	public void setProperties(Vector input);
}
