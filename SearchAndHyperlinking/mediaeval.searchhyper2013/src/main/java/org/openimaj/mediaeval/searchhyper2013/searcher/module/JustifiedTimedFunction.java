package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;

public interface JustifiedTimedFunction extends UnivariateFunction {
	
	public boolean addJustification(String justification);
	
	public List<String> getJustifications();
	
	public float getTime();
	
	public class TimeComparator implements Comparator<JustifiedTimedFunction> {

		@Override
		public int compare(JustifiedTimedFunction arg0,
				JustifiedTimedFunction arg1) {
			return Float.compare(arg0.getTime(), arg1.getTime());
		}

	}
}
