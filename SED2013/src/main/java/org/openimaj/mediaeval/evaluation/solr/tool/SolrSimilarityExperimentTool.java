package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment;

import ch.akuhn.matrix.SparseMatrix;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SolrSimilarityExperimentTool extends SolrSimilarityMatrixClustererExperiment {

	private static SolrSimilarityExperimentToolOptions options;
	private Clusterer<SparseMatrix> clusterer;

	/**
	 * @param similarityFile
	 * @param indexFile
	 * @param start
	 * @param end
	 */
	public SolrSimilarityExperimentTool(String similarityFile,String indexFile, int start, int end) {
		super(similarityFile, indexFile, start, end);
	}

	@Override
	public Clusterer<SparseMatrix> prepareClusterer() {
		return this.clusterer;
	}
	/**
	 * @param namedClusterer
	 */
	public void setNextClusterer(Clusterer<SparseMatrix> namedClusterer) {
		this.clusterer = namedClusterer;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		options = new SolrSimilarityExperimentToolOptions(args);
		
		while(options.hasNextExperiment()){
			options.performNextExperiment();
		}
		
		
	}


}
