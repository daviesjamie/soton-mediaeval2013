package org.openimaj.mediaeval.evaluation.solr.tool;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Option;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.SparseDoubleFV;
import org.openimaj.feature.SparseDoubleFVComparison;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment.SparseMatrixSource;
import org.openimaj.mediaeval.evaluation.solr.tool.IncrementalWeightedMergeMode.IWMSparseMatrixSource;
import org.openimaj.util.array.SparseDoubleArray;

import ch.akuhn.matrix.Matrix;
import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.SparseVector;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.Vector.Entry;

/**
 * An {@link IncrementalWeightedMergeMode} which (once the {@link SparseMatrix} is constructed through
 * weighting) the distances in the matrix are used as vectors and the distances between those vectors
 * is used to construct a new distance matrix. 
 * 
 * Rather than expressing things as "how far they are from each other" we express them as similar
 * if they are similarly similar to their neighbours.
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class MetricSpaceIWMMode extends IncrementalWeightedMergeMode{
	
	@Option(
		name = "--metric-space-distance-metric",
		aliases = "-msdist",
		required = false,
		usage = "The similarity  metric used to turn a similarity matrix into a similarity matrix of distances"
	)
	SparseDoubleFVComparison comp = SparseDoubleFVComparison.COSINE_SIM;
	
	@Option(
		name = "--metric-space-distance-threshold",
		aliases = "-msthresh",
		required = false,
		usage = "The threshold of the similarity metric, below this value sets the similarity of two items to 0. Default thresh is 0 (which is appropriate for the default COSINE_SIM"
	)
	Double threshold = null;
	
	@Option(
		name = "--force-normalisation",
		aliases = "-msnorm",
		required = false,
		usage = "Force the normalisation of the metric space once calculated"
	)
	boolean msnorm = false;

	private int DIAG_VALUE = 1;
	
	
	private static final Logger logger = Logger.getLogger(MetricSpaceIWMMode.class);
	@Override
	public void setup() {
		if(threshold == null){
			if(comp.isDistance()){
				threshold = 100d;
			}
			else{
				threshold = 0d;
			}
		}
		if(comp.isDistance()){
			DIAG_VALUE = 0;
		}
		super.setup();
	}
	
	@Override
	public NamedSolrSimilarityMatrixClustererExperiment nextSimmat() {
		NamedSolrSimilarityMatrixClustererExperiment nsm = super.nextSimmat();
		nsm.name = String.format("msdist=%s/msthresh=%2.2f/%s",this.comp.name(),this.threshold,nsm.name);
		return nsm ;
	}
	@Override
	protected SparseMatrixSource createSparseMatrixSource(double[] curperm,String combname) {
		SparseMatrixSource sps = new IWMSparseMatrixSource(combname, curperm){

			@Override
			public SimilarityMatrixWrapper mat() {
				SimilarityMatrixWrapper combinedMatrix = super.mat();
				SparseMatrix combinedSM = combinedMatrix.matrix();
				SparseMatrix metricSM = (SparseMatrix) combinedSM.newInstance();
				logger.debug("Starting metric space construction");
				double maxd = -Double.MAX_VALUE;
				for (int ri = 0; ri < combinedSM.rowCount(); ri++) {
					metricSM.put(ri, ri, DIAG_VALUE);
					SparseVector spvI = (SparseVector)combinedSM.row(ri);
					SparseDoubleArray rowi = MatlibMatrixUtils.sparseVectorToSparseArray(spvI);
					for (int rj = ri+1; rj < combinedSM.rowCount(); rj++) {
						SparseVector spvJ = (SparseVector) (combinedSM.row(rj));
						SparseDoubleArray rowj = MatlibMatrixUtils.sparseVectorToSparseArray(spvJ);
						double d = comp.compare(rowi, rowj);
						maxd = Math.max(maxd, d);
						if(comp.isDistance()){
							if(d < threshold){
								metricSM.put(ri, rj, d);
								metricSM.put(rj, ri, d);
							}
						}
						else{
							if(d > threshold){
								metricSM.put(ri, rj, d);
								metricSM.put(rj, ri, d);
							}
						}
						
					}
				}
				if(msnorm){
					for (int ri = 0; ri < combinedSM.rowCount(); ri++) {
						Vector row = metricSM.row(ri);
						for (Entry d : row.entries()) {
							if(comp.isDistance())
								row.put(d.index,1 - (d.value/maxd));
							else
								row.put(d.index,d.value/maxd);
						}
					}
				}
				logger.debug("Finished metric space construction");
				return new SimilarityMatrixWrapper(metricSM, combinedMatrix);
			}
		};
		return sps;
	}
}
