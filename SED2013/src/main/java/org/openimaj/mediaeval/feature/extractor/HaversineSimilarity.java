package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;

/**
 * The haversine formalua to determine the great-circle distance between
 * two points on earth.
 *
 * see: http://www.ismll.uni-hildesheim.de/pub/pdfs/Reuter_et_al_ICWSM_2011.pdf
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class HaversineSimilarity implements DoubleFVComparator{
	/**
	 * The default normaliser
	 */
	public HaversineSimilarity() {
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
		double deltaLat = lat2 - lat1;
		double deltaLon = lon2 - lon1;
		double sinLat = Math.sin(deltaLat / 2.);
		double sinLon = Math.sin(deltaLon / 2.);
		double cosLat1 = Math.cos(lat1);
		double cosLat2 = Math.cos(lat2);
		double phi = (sinLat * sinLat) + cosLat1 * cosLat2 * (sinLon * sinLon);
		double atanPhi = Math.atan2(Math.sqrt(phi), Math.sqrt(1-phi));
		double haversine = 2 * atanPhi * atanPhi;
		return 1 - haversine;
	}

}
