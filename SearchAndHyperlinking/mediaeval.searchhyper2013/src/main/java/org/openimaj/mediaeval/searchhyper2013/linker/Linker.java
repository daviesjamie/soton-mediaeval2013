package org.openimaj.mediaeval.searchhyper2013.linker;

import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;

/**
 * A Linker object implements a specific linking algorithm for the purpose of 
 * fulfilling the linking task.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public interface Linker {
	
	public ResultList link(Anchor q) throws LinkerException;

	/**
	 * Provides Linkers with the option for arbitrary external configuration 
	 * in a manner compatible with LinkerEvaluator.
	 * @param settings
	 */
	public void configure(Float[] settings);

	/**
	 * Returns the number of settings configured by this Linker, so as to 
	 * permit simple and de-coupled coding of configure() down the inheritance 
	 * hierarchy.
	 * 
	 * @return
	 */
	int numSettings();
}
