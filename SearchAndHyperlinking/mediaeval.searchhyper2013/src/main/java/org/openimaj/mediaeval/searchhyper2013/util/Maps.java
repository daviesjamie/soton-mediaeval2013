package org.openimaj.mediaeval.searchhyper2013.util;

import java.util.Collection;
import java.util.Map;

public abstract class Maps {
	
	@SuppressWarnings("unchecked")
	public static
	<KEY, VALUE extends Collection<VALUES>, VALUES> 
	void mergeMap(Map<KEY, VALUE> main,
				  Map<KEY, ? extends Collection<? extends VALUES>> other) {
		
		for (KEY key : other.keySet()) {
			Collection<VALUES> collection = main.get(key);
				
			if (collection != null) {
				collection.addAll(other.get(key));
			} else {
				main.put(key, (VALUE) other.get(key));
			}
		}
	}
}
