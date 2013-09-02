package org.openimaj.mediaeval.searchhyper2013.searcher;

/**
 * Encapsulates exceptions thrown by Searchers.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class SearcherException extends Exception {
	
	private static final long serialVersionUID = -7862024234278345683L;

	public SearcherException(Exception e) {
		super(e);
	}
}
