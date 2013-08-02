package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.experiment.evaluation.cluster.ClusterEvaluator;
import org.openimaj.experiment.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.MEClusterAnalyser;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;

import ch.akuhn.matrix.SparseMatrix;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SolrSimilarityMatrixExperiment {

	/**
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, XMLStreamException, ParseException {
		String similarityMatrix = args[0];
		String indexFile = args[1];
		SparseMatrix mat = IOUtils.readFromFile(new File(similarityMatrix));
		int start = 1000;
		int end = 2000;
		MapBackedDataset<Integer, ? extends Dataset<IndexedPhoto>, IndexedPhoto> ds = datasetFromSolr(indexFile,start,end);
		SparseMatrix sub = MatlibMatrixUtils.subMatrix(mat,start,end,start,end);
		new ClusterEvaluator<IndexedPhoto, MEAnalysis>(
			gen,
			datasetToClusters(ds),
			new MEClusterAnalyser()
		);
	}

	private static int[][] datasetToClusters(MapBackedDataset<Integer, ? extends Dataset<IndexedPhoto>, IndexedPhoto> ds)
	{
		return null;
	}

	private static MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> datasetFromSolr(String indexFile, int start, int end) throws CorruptIndexException, IOException {
//		Query q = NumericRangeQuery.newLongRange("index", 0l, 0l, true, true);
////		final Query q = new QueryParser(Version.LUCENE_40, "tag", new StandardAnalyzer(Version.LUCENE_40)).parse("cheese");
		final Directory directory = new SimpleFSDirectory(new File(indexFile));
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		final TopScoreDocCollector collector = TopScoreDocCollector.create(end-start, true);
		Query q = NumericRangeQuery.newLongRange("index", (long)start, (long)end, true, false);
		searcher.search(q, collector);
		final ScoreDoc[] hits = collector.topDocs().scoreDocs;
		MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> ret = new MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto>();
		for (ScoreDoc scoreDoc : hits) {
//			System.out.println(scoreDoc.doc + ": " + scoreDoc.score);
			final Document d = searcher.doc(scoreDoc.doc);
			Photo p = PhotoUtils.createPhoto(d);
//			List<IndexableField> fs = d.getFields();
//			for (IndexableField indexableField : fs) {
//				System.out.println(indexableField.name());
//			}
			long index = (Long) d.getField("index").numericValue();
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
