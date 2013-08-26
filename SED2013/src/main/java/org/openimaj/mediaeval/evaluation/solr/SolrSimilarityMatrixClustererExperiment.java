package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.solr.request.ServletSolrParams;
import org.openimaj.data.dataset.ListBackedDataset;
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
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.util.function.Function;

import ch.akuhn.matrix.SparseMatrix;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public abstract class SolrSimilarityMatrixClustererExperiment implements RunnableExperiment{
	final static Logger logger = Logger.getLogger(SolrSimilarityMatrixClustererExperiment.class);
	private String similarityExp = null;
	private String similarityRoot;
	
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
	 * This constructor makes this experiment load its {@link SparseMatrix} using {@link SED2013SolrSimilarityMatrix#readSparseMatricies(String, String...)}
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
	 * @param indexFile
	 * @param start
	 * @param end
	 * @return dataset from a solr index
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public static MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> datasetFromSolr(String indexFile, int start, int end) throws CorruptIndexException, IOException {
//		Query q = NumericRangeQuery.newLongRange("index", 0l, 0l, true, true);
////		final Query q = new QueryParser(Version.LUCENE_40, "tag", new StandardAnalyzer(Version.LUCENE_40)).parse("cheese");
		final Directory directory = new SimpleFSDirectory(new File(indexFile));
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> ret = new MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto>(){
			@Override
			public String toString() {
				return "Clusters: " + this.size() + " Instances: " + this.numInstances();
			}
		};
		for (int i = start; i < end; i++) {
			Query q = NumericRangeQuery.newLongRange("index", (long)i, (long)i, true, true);
			
			TopDocs docs = searcher.search(q, 1);
			ScoreDoc scoreDoc = docs.scoreDocs[0];
			
			final Document d = searcher.doc(scoreDoc.doc);
			Photo p = PhotoUtils.createPhoto(d);
			long index = (Long) d.getField("index").numericValue()-start;
			long cluster = (Long) d.getField("cluster").numericValue();
			
			ListDataset<IndexedPhoto> clusterList = ret.get((int)cluster);
			if(clusterList==null){
				ret.put((int) cluster, clusterList = new ListBackedDataset<IndexedPhoto>());
			}
			clusterList.add(new IndexedPhoto(index, p));
		}
		return ret ;
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
			if(this.simMatrixFile!=null){		
				try {
					this.similarityMatrixFilename = new File(this.simMatrixFile).getName();
					this.similarityMatrix = new SimilarityMatrixWrapper(this.simMatrixFile, start, end);
				} catch (IOException e) {
				}
			}
			else{
				try {
					SparseMatrix sp = SED2013SolrSimilarityMatrix.readSparseMatricies(similarityRoot, this.similarityExp).get(this.similarityExp);
					this.similarityMatrix = new SimilarityMatrixWrapper(sp, start, end);
					this.similarityMatrixFilename = similarityRoot + "#" + similarityExp;
				} catch (IOException e) {
				}
			}
		}
		this.start = this.similarityMatrix.start();
		this.end = this.similarityMatrix.end();
		if(this.groundtruth == null){
			logger.debug("Querying lucene index");
			try {
				groundtruth = datasetFromSolr(indexFile,start,end);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			logger.debug("Got from index: " + groundtruth.numInstances());
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
