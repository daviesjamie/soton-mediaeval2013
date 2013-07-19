package org.openimaj.mediaeval.feature.extractor;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.feature.FeatureVector;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.util.comparator.DistanceComparator;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public abstract class CombinedFVComparator<T> implements DistanceComparator<T>{


	protected List<ExtractorComparator<T, ? extends FeatureVector>> comps;

	/**
	 * @param comps
	 */
	public CombinedFVComparator(List<ExtractorComparator<T, ? extends FeatureVector>> comps) {
		this.comps = comps;
	}

	@Override
	public double compare(T o1, T o2) {
		List<Double> comparisons = new ArrayList<Double>();
		for (ExtractorComparator<T, ? extends FeatureVector> comp : this.comps) {
			try {
				comparisons.add(comp.doComparison(o1, o2));
			} catch (Exception e) {
				comparisons.add(Double.NaN);
			}
		}
		return aggregate(comparisons);
	}

	/**
	 * @param comparisons list of comparisons, might be null if either of the features were null
	 * @return aggregate the similarities
	 */
	public abstract double aggregate(List<Double> comparisons) ;

	@Override
	public abstract boolean isDistance() ;

	/**
	 * A {@link CombinedFVComparator} which aggregates by getting the mean of the comparisons
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 * @param <T>
	 */
	public static class Mean<T> extends CombinedFVComparator<T>{



		/**
		 * @param excomps
		 */
		public Mean(List<ExtractorComparator<T, ? extends FeatureVector>> excomps) {
			super(excomps);
		}

		@Override
		public double aggregate(List<Double> comparisons) {
			int seen = 0;
			double total = 0;
			for (Double d : comparisons) {
				if(d!=null && !Double.isNaN(d)){
					total+=d;
					seen++;
				}
			}
			return total/seen;
		}

		@Override
		public boolean isDistance() {
			return this.comps.get(0).secondObject().isDistance();
		}

	}

}
