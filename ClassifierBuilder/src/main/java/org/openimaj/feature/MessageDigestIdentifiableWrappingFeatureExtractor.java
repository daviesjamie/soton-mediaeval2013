package org.openimaj.feature;

import java.security.MessageDigest;

import org.openimaj.data.identity.IdentifiableObject;
import org.openimaj.data.identity.MessageDigestIdentifiedObject;
import org.openimaj.feature.FeatureExtractor;

/**
 * Wraps a FeatureExtractor<FEATURE, IdentifiableObject> so as to automatically 
 * identify objects as MessageDigestIdentifiedObjects before passing them 
 * down to the underlying FeatureExtractor.
 * 
 * Useful if you have a DiskCachingFeatureExtractor and you're using otherwise 
 * unidentifiable objects.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <FEATURE>
 * @param <OBJECT>
 */
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
