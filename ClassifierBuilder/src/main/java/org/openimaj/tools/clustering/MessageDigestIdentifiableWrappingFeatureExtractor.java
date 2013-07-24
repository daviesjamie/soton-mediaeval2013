package org.openimaj.tools.clustering;

import java.security.MessageDigest;

import org.openimaj.data.identity.IdentifiableObject;
import org.openimaj.feature.FeatureExtractor;

public class MessageDigestIdentifiableWrappingFeatureExtractor<FEATURE, OBJECT>
		implements FeatureExtractor<FEATURE, OBJECT> {
	
	FeatureExtractor<FEATURE, IdentifiableObject<OBJECT>> featureExtractor;
	MessageDigest md;
	
	public MessageDigestIdentifiableWrappingFeatureExtractor(
			MessageDigest md,
			FeatureExtractor<FEATURE, IdentifiableObject<OBJECT>> featureExtractor) {
		this.md = md;
		this.featureExtractor = featureExtractor;
	}
	
	
	@Override
	public FEATURE extractFeature(OBJECT object) {
		return featureExtractor.extractFeature(new MessageDigestIdentifiedObject<MessageDigest, OBJECT>(md, object));
	}

}
