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
		long takenD = normTimeTaken(p1) - normTimeTaken(p2);
		long addedD = p1.getDatePosted().getTime() - p2.getDatePosted().getTime();
		
		long d = takenD + addedD;
		if(d < 0) return -1;
		else if (d>0) return 1;
		else return 0;
	}

	private long normTimeTaken(Photo p) {
		long t = p.getDateTaken().getTime();
		if(t<1000)t = p.getDatePosted().getTime();
		return t;
	}

}
