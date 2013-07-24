package org.openimaj.tools.clustering;

import org.openimaj.data.identity.IdentifiableObject;
import org.openimaj.feature.FeatureExtractor;

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
	
	/**
	 * TODO:
	 * 
	 *   * write MD5IdentifiedObject class to wrap OBJECT for putting into the 
	 *     DiskCachingFeatureExtractor<OBJECT, IdentifiableObject<OBJECT>> 
	 *     with the IdentifiableObjectUnwrappingFeaturExtractor<FEATURE, OBJECT>.
	 */

}
