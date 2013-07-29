package org.openimaj.feature;

import org.openimaj.data.identity.IdentifiableObject;
import org.openimaj.feature.FeatureExtractor;

/**
 * Wraps a FeatureExtractor<FEATURE, OBJECT> to handle 
 * IdentifiableObject<OBJECT>'s by unwrapping them and passing their OBJECT to 
 * the underlying FeatureExtractor.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <FEATURE>
 * @param <OBJECT>
 */
public class IdentifiableObjectUnwrappingFeatureExtractor<FEATURE, OBJECT> implements
		FeatureExtractor<FEATURE, IdentifiableObject<OBJECT>> {
	private FeatureExtractor<FEATURE, OBJECT> extractor;
	
	public IdentifiableObjectUnwrappingFeatureExtractor(FeatureExtractor<FEATURE, OBJECT> extractor) {
		this.extractor = extractor;
	}
	
	@Override
	public FEATURE extractFeature(IdentifiableObject<OBJECT> object) {
		return extractor.extractFeature(object.data);
	}
}
