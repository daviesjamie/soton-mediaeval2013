package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.experiment.evaluation.cluster.ClusterEvaluator;
import org.openimaj.experiment.evaluation.cluster.analyser.FullMEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FullMEClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.analyser.SimpleMEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.SimpleMEClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.util.function.Function;

import ch.akuhn.matrix.SparseMatrix;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SolrSimilarityMatrixExperiment {
	final static Logger logger = Logger.getLogger(SolrSimilarityMatrixExperiment.class);
	/**
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, XMLStreamException, ParseException {
		String similarityMatrix = args[0];
		String indexFile = args[1];
		logger.debug("Loading psarse matrix");
		SparseMatrix mat = IOUtils.readFromFile(new File(similarityMatrix));
		int start = 0;
		int end = -1;
		if(end < 0) end = mat.columnCount() + end + 1;
		logger.debug("Querying lucene index");
		MapBackedDataset<Integer, ? extends ListDataset<IndexedPhoto>, IndexedPhoto> ds = datasetFromSolr(indexFile,start,end);
		logger.debug("Got from index: " + ds.numInstances());
		logger.debug("Extracting submatrix");
		SparseMatrix sub = MatlibMatrixUtils.subMatrix(mat,start,end,start,end);
		logger.debug(String.format("Submatrix dims: %d x %d" ,sub.rowCount(),sub.columnCount()));
		Clusterer<SparseMatrix> gen = new SimilarityDBSCAN(0.6, 5);
		Function<IndexedPhoto,Integer> func = new Function<IndexedPhoto, Integer>() {
			
			@Override
			public Integer apply(IndexedPhoto in) {
				return (int) in.first;
			}
		};
		logger.debug("Preparing evaluation");
		ClusterEvaluator<SparseMatrix, SimpleMEAnalysis> a = new ClusterEvaluator<SparseMatrix, SimpleMEAnalysis>(
			gen,
			sub,
			func,
			ds,
			new SimpleMEClusterAnalyser()
		);
		logger.debug("Evaluating clusterer");
		SimpleMEAnalysis analysis = a.analyse(a.evaluate());
		logger.debug("Done! Printing.");
		System.out.println(analysis.getSummaryReport());
	}

	private static MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> datasetFromSolr(String indexFile, int start, int end) throws CorruptIndexException, IOException {
//		Query q = NumericRangeQuery.newLongRange("index", 0l, 0l, true, true);
////		final Query q = new QueryParser(Version.LUCENE_40, "tag", new StandardAnalyzer(Version.LUCENE_40)).parse("cheese");
		final Directory directory = new SimpleFSDirectory(new File(indexFile));
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> ret = new MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto>();
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
}
