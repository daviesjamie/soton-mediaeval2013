package org.openimaj.mediaeval.feature.extractor;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.util.comparator.DistanceComparator;
import org.openimaj.util.pair.IndependentPair;

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
	 * @param <D> The type
	 */
	public static class ExtractorComparator<T,F,D> extends IndependentPair<FeatureExtractor<F,T>, D>{

		/**
		 * @param obj1
		 * @param obj2
		 */
		public ExtractorComparator(FeatureExtractor<F, T> obj1, D obj2) {
			super(obj1, obj2);
		}

	}
	private DoubleFV[][] datasetFV;
	private List<ExtractorComparator<T, DoubleFV, DoubleFVComparator>> exComp;

	/**
	 * Hidden on purpose
	 * @param ds
	 */
	DatasetSimilarity(Dataset<T> ds) {
	}
	/***
	 * Extract the fvs features from each item in the ds. The {@link DoubleFVComparison#COSINE_SIM} is used as the {@link DistanceComparator}
	 * @param ds
	 * @param fvs
	 */
	public DatasetSimilarity(Dataset<T> ds, FeatureExtractor<DoubleFV, T> ... fvs) {
		this.exComp = new ArrayList<ExtractorComparator<T,DoubleFV,DoubleFVComparator>>(fvs.length);
		for (int i = 0; i < fvs.length; i++) {
			exComp.add(
				new ExtractorComparator<T, DoubleFV, DoubleFVComparator>(
					fvs[i], DoubleFVComparison.COSINE_SIM
				)
			);
		}
		this.prepareFV(ds, exComp);
	}
	/**
	 * Extract the fvs from each item in the ds.
	 *
	 * @param ds the dataset from which to create feature vectors
	 * @param comps a list of extractor/comparator pairs
	 */
	public DatasetSimilarity(Dataset<T> ds, List<ExtractorComparator<T,DoubleFV,DoubleFVComparator>>comps) {
		prepareFV(ds, comps);
	}
	private void prepareFV(Dataset<T> ds, List<ExtractorComparator<T, DoubleFV, DoubleFVComparator>> exComp) {
		this.exComp = exComp;
		this.datasetFV = new DoubleFV[ds.numInstances()][exComp.size()];
		Iterator<T> dsIter = ds.iterator();
		logger.info("Extracting dataset features");
		for (int i = 0; i < datasetFV.length; i++) {
			if(i%1000 == 0){
				logger.info("Done: " + i);
			}
			T next = dsIter.next();
			for (int j = 0; j < exComp.size(); j++) {
				this.datasetFV[i][j] = exComp.get(j).firstObject().extractFeature(next);
			}
		}
	}

	@Override
	public SparseMatrix extractFeature(T object) {

		DoubleFV[] objFVS = new DoubleFV[this.exComp.size()];
		for (int j = 0; j < objFVS.length; j++) {
			// Get the j'th extractor and hold the FV
			objFVS[j] = this.exComp.get(j).firstObject().extractFeature(object);
		}
		return simMatrix(objFVS);
	}
	private SparseMatrix simMatrix(DoubleFV[] objFVS) {
		SparseMatrix ret = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(
				this.datasetFV.length, this.exComp.size()
				);
		for (int i = 0; i < this.datasetFV.length; i++) {
			for (int j = 0; j < objFVS.length; j++) {
				// if the j'th fv for objFVS is null, the similarity is NaN
				// if the j'th fv for the i'th dataset entry is null, similarity is also NaN
				if(objFVS[j] == null || datasetFV[i][j] == null){
					ret.setElement(i, j, Double.NaN);
					continue;
				}
				// Get the j'th comparator and compare the FVs
				DoubleFVComparator comp = this.exComp.get(j).secondObject();
				double similarity = comp.compare(
					objFVS[j].values,datasetFV[i][j].values
				);
				if(comp.isDistance()) similarity = -similarity;
				if(similarity != 0){
					ret.setElement(i, j, similarity);
				}
			}
		}
		return ret;
	}

	/**
	 * @param index
	 * @return extract the feature from the i'th dataset
	 */
	public SparseMatrix extractFeature(int index) {
		DoubleFV[] objFVS = this.datasetFV[index];
		return simMatrix(objFVS);
	}

}
