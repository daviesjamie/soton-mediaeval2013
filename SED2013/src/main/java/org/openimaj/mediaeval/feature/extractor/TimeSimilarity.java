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
		// This means its similarity is less than 1 minute, so the same 
		if(d<=1)return 1.0;
		double add = Math.log(d) / normaliser;
		if(add < 0){
			System.out.println("WHAT THE FUCK");
		}
		return 1.0 - add;
	}
	
	public static void main(String[] args) {
		DoubleFV t1 = new DoubleFV(new double[]{0});
		DoubleFV t2 = new DoubleFV(new double[]{1000 * 60 * 60 * 24});
		
		System.out.println(new TimeSimilarity().compare(t1, t2));
	}

}
