package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;

/**
 * The equirectangular approximation. This is the fastest of the 3 spherical
 * similarity metrics but apparently the least accurate
 *
 * see: http://www.movable-type.co.uk/scripts/latlong.html
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class EquirectangularSimilarity implements DoubleFVComparator{
	/**
	 * The default normaliser
	 */
	public EquirectangularSimilarity() {
	}

	@Override
	public double compare(DoubleFV o1, DoubleFV o2) {
		return compare(o1.values,o2.values);
	}

	@Override
	public boolean isDistance() {
		return false;
	}

	@Override
	public double compare(double[] h1, double[] h2) {
		if(Double.isNaN(h1[0] + h2[0] + h1[1] + h2[1])) return Double.NaN;
		double lat2 = Math.toRadians(h2[0]);
		double lat1 = Math.toRadians(h1[0]);
		double lon2 = Math.toRadians(h2[1]);
		double lon1 = Math.toRadians(h1[1]);

		double x = (lon2-lon1) * Math.cos((lat1+lat2)/2);
		double y = (lat2-lat1);

		double d = Math.sqrt(x*x + y*y) ;
		return 1-d;
	}

}
