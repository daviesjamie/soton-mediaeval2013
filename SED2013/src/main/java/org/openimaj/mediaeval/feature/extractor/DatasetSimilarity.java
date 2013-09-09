package org.openimaj.mediaeval.feature.extractor;


import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.FeatureVector;
import org.openimaj.util.comparator.DistanceComparator;
import org.openimaj.util.pair.IndependentPair;

import ch.akuhn.matrix.SparseMatrix;

/**
 * Give a set of {@link FeatureExtractor}, extract a feature from every item in
 * the {@link Dataset}. The extracted feature this class produces is a {@link SparseMatrix}
 * which contains the result of a {@link DoubleFVComparator} given each item in the {@link Dataset}
 * compared to the instance given a particular feature vector
 *
 * But another way, give a T instance t
 *
 * extractFeature(t) = mat (SparseMatrix(n,f))
 *
 * s.t. n is the number of items int he datasets and f is the number of feature extractors
 * and mat(i,j) contains the similarity of t with the i'th dataset item using the j'th feature extractor
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <T>
 *
 */
public class DatasetSimilarity<T> implements FeatureExtractor<SparseMatrix, T>{
	Logger logger = Logger.getLogger(DatasetSimilarity.class);
	/**
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 * @param <T> The type of data the extractor works on
	 * @param <F> The type of feature the extractor produces
	 */
	public static class ExtractorComparator<T,F> extends IndependentPair<FeatureExtractor<F,T>, DistanceComparator<F>>{

		/**
		 * @param obj1
		 * @param obj2
		 */
		public ExtractorComparator(FeatureExtractor<F, T> obj1, DistanceComparator<F> obj2) {
			super(obj1, obj2);
		}

		/**
		 * @param t1
		 * @param t2
		 * @return perform the held comparison using the held extractor. Returns {@link Double#NaN} if the comparison is invalid
		 */
		public double doComparison(T t1, T t2){
			F f1 = this.firstObject().extractFeature(t1);
			F f2 = this.firstObject().extractFeature(t2);
			if(f1 == null || f2 == null) return Double.NaN;
			double comp = this.secondObject().compare(f1, f2);
			return comp;
		}

		@Override
		public String toString() {
			return this.firstObject().toString();
		}
	}
	private List<ExtractorComparator<T, ? extends FeatureVector>> exComp;
	private int[] compMod;
	private int datasetSize;
	private Dataset<T> ds;

	/**
	 * Hidden on purpose
	 * @param ds
	 */
	DatasetSimilarity(Dataset<T> ds) {
	}
	/**
	 * Extract the fvs from each item in the ds.
	 *
	 * @param ds the dataset from which to create feature vectors
	 * @param comps a list of extractor/comparator pairs
	 */
	public DatasetSimilarity(Dataset<T> ds, List<ExtractorComparator<T,? extends FeatureVector>>comps) {
		prepareFV(ds, comps);
	}
	private void prepareFV(Dataset<T> ds, List<ExtractorComparator<T, ? extends FeatureVector>> exComp) {
		this.ds = ds;
		this.exComp = exComp;
		this.compMod = new int[exComp.size()];
		for (int i = 0; i < this.compMod.length; i++) {
			compMod[i] = exComp.get(i).secondObject().isDistance() ? -1 : 1;
		}


		Iterator<T> dsIter = ds.iterator();
		logger.info("Extracting dataset features");
		this.datasetSize = ds.numInstances();
		for (int i = 0; i < datasetSize; i++) {
			if(i%1000 == 0){
				logger.info("Done: " + i);
			}
			T next = dsIter.next();
			for (int j = 0; j < exComp.size(); j++) {
				exComp.get(j).firstObject().extractFeature(next);
			}
		}
	}

	@Override
	public SparseMatrix extractFeature(T object) {
		return simMatrix(object);
	}
	private SparseMatrix simMatrix(T object) {
		SparseMatrix ret = new SparseMatrix(this.datasetSize, this.exComp.size());
		int i = 0;
		for (T dsItem: this.ds) {
			for (int j = 0; j < this.exComp.size(); j++) {
				try{
					double similarity = this.exComp.get(j).doComparison(object,dsItem);
					if(similarity != 0 && !Double.isNaN(similarity)){
						ret.put(i, j, similarity * this.compMod[j]);
					}

				}
				catch(Exception e){
					continue;
				}
			}
			i++;
		}
		return ret;
	}


}
