package org.openimaj.mediaeval.feature.extractor;

import java.util.Date;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

import com.aetrion.flickr.photos.Photo;

/**
 * Construct a feature representing the time of the photo in some way.
 * Constructs a 3D FV using: {@link Photo#getDateAdded()}, {@link Photo#getDatePosted()} and {@link Photo#getDateTaken()}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoTime implements FeatureExtractor<DoubleFV, Photo>{

	@Override
	public DoubleFV extractFeature(Photo p) {
		DoubleFV ret = new DoubleFV(3);
		Date dateAdded = p.getDateAdded();
		if(dateAdded!=null) ret.values[0] = dateAdded.getTime();
		Date datePosted = p.getDatePosted();
		if(datePosted!=null) ret.values[1] = datePosted.getTime();
		Date dateTaken = p.getDateTaken();
		if(dateTaken!=null) ret.values[2] = dateTaken.getTime();
		return ret ;
	}

}
