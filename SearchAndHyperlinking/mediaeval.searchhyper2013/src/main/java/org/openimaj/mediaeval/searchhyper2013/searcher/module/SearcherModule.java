package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;

public interface SearcherModule {
	
	public TimelineSet search(Query q, TimelineSet currentSet)
													throws SearcherException;
}
