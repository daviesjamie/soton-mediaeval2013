package org.openimag.mediaeval.searchandhyperlinking;

import java.util.List;

/**
 * Represents classes that can search queries.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public interface Searcher {
	public List<Result> search(Query q);
}
