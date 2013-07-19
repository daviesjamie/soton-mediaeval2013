package org.openimaj.mediaeval.evaluation.datasets;

import org.apache.log4j.Logger;
import org.openimaj.data.RandomData;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.mediaeval.feature.extractor.EquirectangularSimilarity;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013SanityTest {
	static Logger logger = Logger.getLogger(SED2013SanityTest.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int nItems = 300000;
		logger.info("Creating data");
		double[][] items = new double[nItems][];
		logger.info("Filling data");
		for (int i = 0; i < items.length; i++) {
			items[i] = RandomData.getRandomDoubleArray(2, -90, 90);
		}
		logger.info("comparing");
		DoubleFVComparator sim = new EquirectangularSimilarity();
		for (int i = 0; i < items.length; i++) {
			if(i%100==0){
				logger.info("Row: " + i);
			}
			for (int j = i; j < items.length; j++) {
				sim.compare(items[i], items[j]);
			}
		}
	}
}
