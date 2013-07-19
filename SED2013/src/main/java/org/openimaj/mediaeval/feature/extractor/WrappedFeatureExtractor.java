package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.FeatureExtractor;
import org.openimaj.util.function.Function;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <FEATURE>
 * @param <DATA>
 * @param <INNER_DATA>
 */
public class WrappedFeatureExtractor<FEATURE, DATA, INNER_DATA> implements FeatureExtractor<FEATURE, DATA> {

	private Function<DATA, INNER_DATA> fun;
	private FeatureExtractor<FEATURE, INNER_DATA> fe;

	/**
	 * @param fe
	 * @param fun
	 */
	public WrappedFeatureExtractor(FeatureExtractor<FEATURE,INNER_DATA> fe, Function<DATA, INNER_DATA> fun) {
		this.fun = fun;
		this.fe = fe;
	}

	@Override
	public FEATURE extractFeature(DATA object) {
		return this.fe.extractFeature(this.fun.apply(object));
	}

}
