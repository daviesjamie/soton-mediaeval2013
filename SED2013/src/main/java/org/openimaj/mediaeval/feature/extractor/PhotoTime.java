package org.openimaj.mediaeval.feature.extractor;

import java.util.Date;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
/**
 * Construct a feature representing the time of the photo in some way.
 * Constructs a 3D FV using: {@link Photo#getDateAdded()}, {@link Photo#getDatePosted()} and {@link Photo#getDateTaken()}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoTime implements FeatureExtractor<DoubleFV, Photo>{
	/**
	 * The type of PhotoTime feature
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public static enum Type{
		/**
		 * The time added
		 */
		ADDED,
		/**
		 * The time uploaded
		 */
		POSTED,
		/**
		 * The time taken
		 */
		TAKEN,
		/**
		 * all 3 types of time (fv.length == 3)
		 */
		ALL,
	}
	private Type type;
	/**
	 * Produce features containing all time
	 */
	public PhotoTime() {
		this.type = Type.ALL;
	}
	/**
	 * Produce features of this time type
	 * @param t
	 */
	public PhotoTime(Type t){
		this.type = t;
	}
	@Override
	public DoubleFV extractFeature(Photo p) {
		DoubleFV ret = new DoubleFV(3);
		Date dateAdded = p.getDateAdded();
		if(dateAdded!=null) {
			ret.values[0] = dateAdded.getTime();
		}else{
			ret.values[0] = Double.NaN;
		}
		Date datePosted = p.getDatePosted();
		if(datePosted!=null) {
			ret.values[1] = datePosted.getTime();
		}else{
			ret.values[1] = Double.NaN;
		}
		Date dateTaken = p.getDateTaken();
		if(dateTaken!=null) {
			ret.values[2] = dateTaken.getTime();
		}else{
			ret.values[2] = Double.NaN;
		}
		if(type.equals(Type.ALL)){
			return ret;
		}
		else{
			DoubleFV subret = new DoubleFV(1);
			subret.values[0] = ret.values[this.type.ordinal()];
			ret = subret;
		}
		return ret ;
	}

}
