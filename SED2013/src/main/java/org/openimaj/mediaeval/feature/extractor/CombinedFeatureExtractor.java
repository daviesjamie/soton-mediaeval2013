package org.openimaj.mediaeval.feature.extractor;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.FeatureVector;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;

/**
 * A similarity aggregator uses an underlying {@link FeatureExtractor} which
 * produces a {@link List} of {@link DoubleFV} instances which it produces a
 * single dista
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <T>
 *
 */
public class CombinedFeatureExtractor<T> implements FeatureExtractor<List<FeatureVector>, T> {

	private List<ExtractorComparator<T, ? extends FeatureVector>> excomps;

	/**
	 * @param excomps
	 */
	public CombinedFeatureExtractor(List<ExtractorComparator<T, ? extends FeatureVector>> excomps) {
		this.excomps = excomps;
	}
	@Override
	public List<FeatureVector> extractFeature(T object) {
		List<FeatureVector> l = new ArrayList<FeatureVector>();
		for (ExtractorComparator<T, ? extends FeatureVector> fd : this.excomps) {
			l.add(fd.firstObject().extractFeature(object));
		}
		return l;
	}
}
