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
public class LogNormalisedHaversineSimilarity extends HaversineSimilarity{
	private double lognorm;
	private double EARTH_RADIUS_METERS = 6372800 ; 

	/**
	 * The default normaliser
	 */
	public LogNormalisedHaversineSimilarity() {
		double basehav = super.haversine(new double[]{0,0}, new double[]{1,0}) * EARTH_RADIUS_METERS;
		lognorm = Math.log(basehav);
	}
	
	/**
	 * The default normaliser
	 * @param lognorm 
	 */
	public LogNormalisedHaversineSimilarity(double meters) {
		this.lognorm = Math.log(meters);
	}

	public LogNormalisedHaversineSimilarity(double[] ds1, double[] ds2) {
		this.lognorm = Math.log(haversine(ds1,ds2) * EARTH_RADIUS_METERS);
	}

	@Override
	public double compare(double[] h1, double[] h2) {
		double meterDistance = super.haversine(h1, h2) * EARTH_RADIUS_METERS;
		if(meterDistance <= 1) return 1;
		double retval = 1 - (Math.log(meterDistance)/lognorm);
		if(retval <= 0) return 0;
		return retval;
	}

}
