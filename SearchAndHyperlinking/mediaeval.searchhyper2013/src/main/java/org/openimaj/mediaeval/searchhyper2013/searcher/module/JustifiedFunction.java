package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;

public interface JustifiedFunction extends UnivariateFunction {
	public boolean addJustification(String justification);
	
	public List<String> getJustifications();
}
