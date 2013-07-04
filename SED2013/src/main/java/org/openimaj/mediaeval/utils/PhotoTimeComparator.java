package org.openimaj.mediaeval.utils;

import java.util.Comparator;

import com.aetrion.flickr.photos.Photo;

/**
 * Combine the effects of date taken and date posted of {@link Photo} instances,
 * ordering photos in some rought chronological order accordingly 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoTimeComparator implements Comparator<Photo>{

	@Override
	public int compare(Photo p1, Photo p2) {
		long takenD = p1.getDateTaken().getTime() - p2.getDateTaken().getTime();
		long addedD = p1.getDatePosted().getTime() - p2.getDatePosted().getTime();
		
		long d = takenD + addedD;
		if(d < 0) return -1;
		else if (d>0) return 1;
		else return 0;
	}

}
