package org.openimaj.mediaeval.feature.extractor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openimaj.data.RandomData;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.time.Timer;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestGeoSimilarity {
	double[][] places = new double[][]{
		new double[]{40.7,-74.01}, // Newyork 0
		new double[]{44.6,-63.6}, // Halifax 1
		new double[]{51.3,-0.2}, // London 2
		new double[]{52.5,13.5}, // Berlin 3
		new double[]{55.6,37.8}, // Moscow 4
		new double[]{54.8,82.9}, // Novosbirsk 5
		new double[]{39.8,116.6}, // Beijing 6
		new double[]{35.5,140.0}, // Tokyo 7
		new double[]{-36,174}, // Auckland 8
	};
	/**
	 *
	 */
	@Test
	public void testHaversine(){
		HaversineSimilarity sim = new HaversineSimilarity();
		assertTrue(sim.compare(places[0], places[1]) > sim.compare(places[0], places[2]));
		assertTrue(sim.compare(places[2], places[3]) > sim.compare(places[3], places[4]));
		assertTrue(sim.compare(places[8], places[7]) > sim.compare(places[8], places[0]));
	}

	@Test
	public void testCosLaw(){
		LawOfCosineSimilarity sim = new LawOfCosineSimilarity();
		assertTrue(sim.compare(places[0], places[1]) > sim.compare(places[0], places[2]));
		assertTrue(sim.compare(places[2], places[3]) > sim.compare(places[3], places[4]));
		assertTrue(sim.compare(places[8], places[7]) > sim.compare(places[8], places[0]));
	}
	@Test
	public void testEquirectangular(){
		EquirectangularSimilarity sim = new EquirectangularSimilarity();
		assertTrue(sim.compare(places[0], places[1]) > sim.compare(places[0], places[2]));
		assertTrue(sim.compare(places[2], places[3]) > sim.compare(places[3], places[4]));
		assertTrue(sim.compare(places[8], places[7]) > sim.compare(places[8], places[0]));
	}

	@Test
	public void loadTest(){
		DoubleFVComparator sim = new HaversineSimilarity();
		Timer t = Timer.timer();
		double n = 100000;
		for (int i = 0; i < n; i++) {
			double[] lats = RandomData.getRandomDoubleArray(2, -90, 90);
			double[] longs = RandomData.getRandomDoubleArray(2, -180, 180);
			double[] d1 = new double[]{lats[0],longs[0]};
			double[] d2 = new double[]{lats[1],longs[1]};
			sim.compare(d1, d2);
		}
		System.out.println("Haversine: " + t.duration()/n);
		sim = new LawOfCosineSimilarity();
		t = Timer.timer();
		for (int i = 0; i < n; i++) {
			double[] lats = RandomData.getRandomDoubleArray(2, -90, 90);
			double[] longs = RandomData.getRandomDoubleArray(2, -180, 180);
			double[] d1 = new double[]{lats[0],longs[0]};
			double[] d2 = new double[]{lats[1],longs[1]};
			sim.compare(d1, d2);
		}
		System.out.println("Cosine: " + t.duration()/n);
		sim = new EquirectangularSimilarity();
		t = Timer.timer();
		for (int i = 0; i < n; i++) {
			double[] lats = RandomData.getRandomDoubleArray(2, -90, 90);
			double[] longs = RandomData.getRandomDoubleArray(2, -180, 180);
			double[] d1 = new double[]{lats[0],longs[0]};
			double[] d2 = new double[]{lats[1],longs[1]};
			sim.compare(d1, d2);
		}
		System.out.println("Equirectangular: " + t.duration()/n);
	}
}