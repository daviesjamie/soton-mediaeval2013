package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.FeatureExtractor;

import com.aetrion.flickr.photos.Photo;


/**
 * The photos id extracted from {@link Photo#getId()}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoId implements FeatureExtractor<String, Photo>{

	@Override
	public String extractFeature(Photo object) {
		return object.getId();
	}
}
