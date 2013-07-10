package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

import com.aetrion.flickr.photos.Photo;

/**
 * Construct a feature representing the latitude and longitude of a {@link Photo}
 * returns null if no geo data exists
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoGeo implements FeatureExtractor<DoubleFV, Photo>{

	@Override
	public DoubleFV extractFeature(Photo p) {
		DoubleFV ret = null;
		if(p.hasGeoData()){
			ret = new DoubleFV(2);
			ret.values[0] = p.getGeoData().getLatitude();
			ret.values[1] = p.getGeoData().getLongitude();
		}
		return ret;
	}

}
