package org.openimaj.mediaeval.searchhyper2013.linker.module;

import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.linker.LinkerException;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;

public interface LinkerModule {
	
	public TimelineSet search(Anchor q, TimelineSet currentSet)
													throws LinkerException;
}
