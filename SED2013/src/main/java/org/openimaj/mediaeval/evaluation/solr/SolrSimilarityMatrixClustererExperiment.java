package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.RunnableExperiment;
import org.openimaj.experiment.annotations.DependentVariable;
import org.openimaj.experiment.annotations.IndependentVariable;
import org.openimaj.experiment.evaluation.cluster.ClusterEvaluator;
import org.openimaj.experiment.evaluation.cluster.analyser.ClusterStatsAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.DecisionAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FScoreAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.PurityAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.RandomBaselineClusterAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.RandomBaselineSMEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.RandomBaselineSMEClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.analyser.RandomIndexAnalysis;
import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.mediaeval.data.util.SimilarityMatrixReader;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.util.function.Function;

import ch.akuhn.matrix.SparseMatrix;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public abstract class SolrSimilarityMatrixClustererExperiment implements RunnableExperiment{
	final static Logger logger = Logger.getLogger(SolrSimilarityMatrixClustererExperiment.class);
	private String similarityExp = null;
	private String similarityRoot;
	private SparseMatrixSource sparseMatrixSource;
	
	/**
	 * @param similarityFile
	 * @param indexFile
	 * @param end 
	 * @param start 
	 */
	public SolrSimilarityMatrixClustererExperiment(String similarityFile, String indexFile, int start, int end) {
		this.start = start;
		this.end = end;
		this.simMatrixFile = similarityFile;
		this.indexFile = indexFile;
	}
	
	/**
	 * This constructor makes this experiment load its {@link SparseMatrix} using {@link SimilarityMatrixReader#readSparseMatricies(String, String...)}
	 * 
	 * @param similarityRoot the root of similarity matricies  
	 * @param similarityExp the particular similarity matrix
	 * @param indexFile
	 * @param end 
	 * @param start 
	 */
	public SolrSimilarityMatrixClustererExperiment(String similarityRoot, String similarityExp, String indexFile, int start, int end) {
		this.start = start;
		this.end = end;
		this.similarityRoot = similarityRoot;
		this.similarityExp = similarityExp;
		this.indexFile = indexFile;
	}
	
	/**
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public static interface SparseMatrixSource{
		/**
		 * @return name of a sparse matrix source
		 */
		public String name();
		/**
		 * @return the sparse matrix
		 */
		public SimilarityMatrixWrapper mat();
	}
	
	/**
	 * @param source 
	 * @param indexFile
	 */
	public SolrSimilarityMatrixClustererExperiment(SparseMatrixSource source, String indexFile) {
		this.sparseMatrixSource = source;
		this.indexFile = indexFile;
	}



	private String simMatrixFile;
	private String indexFile;
	
	@IndependentVariable
	private Clusterer<SparseMatrix> gen;
	
	@IndependentVariable
	protected SimilarityMatrixWrapper similarityMatrix;
	
	@IndependentVariable
	private Function<IndexedPhoto, Integer> transformFunction;
	
	@IndependentVariable
	private MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto>  groundtruth;
	
	@IndependentVariable
	private int start = 0;
	
	@IndependentVariable
	private String similarityMatrixFilename;
	
	@IndependentVariable
	private int end = 1000;
	
//	@DependentVariable
	/**
	 * 
	 */
	public RandomBaselineSMEAnalysis analysis;
	@DependentVariable
	protected RandomBaselineClusterAnalysis<FScoreAnalysis> f1score;
	@DependentVariable
	protected RandomBaselineClusterAnalysis<PurityAnalysis> purity;
	@DependentVariable
	protected RandomBaselineClusterAnalysis<RandomIndexAnalysis> randIndex;
	@DependentVariable
	protected ClusterStatsAnalysis stats;
	@DependentVariable
	protected DecisionAnalysis decision;
	
	
	
	
	

	@Override
	public void setup() {
		if(this.similarityMatrix == null){
			if(this.sparseMatrixSource != null){
				this.similarityMatrixFilename = sparseMatrixSource.name();
				this.similarityMatrix = sparseMatrixSource.mat();
			}
			else if(this.simMatrixFile!=null){		
				try {
					this.similarityMatrixFilename = new File(this.simMatrixFile).getName();
					this.similarityMatrix = new SimilarityMatrixWrapper(this.simMatrixFile, start, end);
				} catch (IOException e) {
				}
			}
			else{
				try {
					this.similarityMatrix = SimilarityMatrixReader.readSparseMatricies(start,end,similarityRoot, this.similarityExp).get(this.similarityExp);;
					this.similarityMatrixFilename = similarityRoot + "#" + similarityExp;
				} catch (IOException e) {
				}
			}
		}
		this.start = this.similarityMatrix.start();
		this.end = this.similarityMatrix.end();
		if(this.groundtruth == null){
			if(indexFile!=null){				
				logger.debug("Querying lucene index");
				try {
					groundtruth = SED2013IndexUtils.datasetFromSolr(indexFile,start,end);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				logger.debug("Got from index: " + groundtruth.numInstances());
			}else{
				this.groundtruth = new MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto>();
			}
		}
		gen = prepareClusterer();
		transformFunction = new Function<IndexedPhoto, Integer>() {
			
			@Override
			public Integer apply(IndexedPhoto in) {
				return (int) in.first;
			}
			
			@Override
			public String toString() {
				return "Index extracting function";
			}
		};
	}

	/**
	 * @return the {@link Clusterer} used in this experiment
	 */
	public abstract Clusterer<SparseMatrix> prepareClusterer();

	@Override
	public void perform() {
		logger.debug("Preparing evaluation");
		if(indexFile!=null){
			logger.debug("Running groundtruth evaluation");
			ClusterEvaluator<SparseMatrix, RandomBaselineSMEAnalysis> a = new ClusterEvaluator<SparseMatrix, RandomBaselineSMEAnalysis>(
					gen,
					similarityMatrix.matrix(),
					transformFunction,
					groundtruth,
					new RandomBaselineSMEClusterAnalyser()
					);
			logger.debug("Evaluating clusterer");
			this.analysis = a.analyse(a.evaluate());
		}
		else{
			logger.debug("Running test evaluation");
			int[][] estimated = gen.performClustering(similarityMatrix.matrix());
			this.analysis = new RandomBaselineSMEClusterAnalyser().analyse(estimated, estimated);
			
		}
	}

	@Override
	public void finish(ExperimentContext context) {
		this.f1score = this.analysis.fscore;
		this.purity = this.analysis.purity;
		this.randIndex = this.analysis.randIndex;
		this.stats = this.analysis.stats;
		this.decision = this.f1score.getUnmodified().getDecisionAnalysis();
	}

	/**
	 * Write the index in each cluster in the format:
	 * index1 cluster1
	 * index2 cluster1
	 * ...
	 * indexk cluster2
	 * ...
	 * indexm clustern
	 * 
	 * @param clusterWriter
	 * @param clusters
	 */
	public void writeIndexClusters(PrintWriter clusterWriter, int[][] clusters) {
		for (int i = 0; i < clusters.length; i++) {
			int[] cluster = clusters[i];
			for (int j = 0; j < cluster.length; j++) {
				clusterWriter.printf("%d %d\n",cluster[j],i);
			}
		}
		clusterWriter.flush();
	}

	/**
	 * @return the ground truth
	 */
	public MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> getGroundtruth() {
		return groundtruth;
	}
}
