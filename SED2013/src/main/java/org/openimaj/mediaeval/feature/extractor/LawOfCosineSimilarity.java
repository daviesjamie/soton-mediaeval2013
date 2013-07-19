package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;

/**
 * Use the spherical law of cosines formula to calculate similarity.
 * Slightly faster than {@link HaversineSimilarity} but slower than {@link EquirectangularSimilarity}.
 * Accurate to within 1 meter distance in a 64 bit system
 *
 * see: http://www.movable-type.co.uk/scripts/latlong.html
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class LawOfCosineSimilarity implements DoubleFVComparator{
	/**
	 * The default normaliser
	 */
	public LawOfCosineSimilarity() {
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

		double d = Math.acos(Math.sin(lat1)*Math.sin(lat2) +
                Math.cos(lat1)*Math.cos(lat2) *
                Math.cos(lon2-lon1));
		return 1-d;
	}

}
