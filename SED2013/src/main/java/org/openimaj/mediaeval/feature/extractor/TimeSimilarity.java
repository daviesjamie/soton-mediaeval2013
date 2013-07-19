package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;

/**
 * A normalised similarity of time. The normaliser defaults to the
 * log of the number of milliseconds in a year.
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TimeSimilarity implements DoubleFVComparator{

	private double normaliser = Math.log(60 * 24 * 365);
	/**
	 * The default normaliser
	 */
	public TimeSimilarity() {
	}

	/**
	 * A perscribed normaliser
	 * @param normaliser
	 */
	public TimeSimilarity(double normaliser) {
		this.normaliser = normaliser;
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
		double h1n = h1[0] / (60 * 1000);
		double h2n = h2[0] / (60 * 1000);
		double d = Math.abs(h1n - h2n);
		if(d==0)return 1.0;
		return 1.0 - (Math.log(d) / normaliser );
	}

}
