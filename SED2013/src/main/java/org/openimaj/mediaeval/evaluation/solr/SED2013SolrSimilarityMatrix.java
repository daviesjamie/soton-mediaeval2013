package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.openimaj.feature.FeatureVector;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.mediaeval.data.SolrDocumentToIndexedPhoto;
import org.openimaj.mediaeval.data.SolrStream;
import org.openimaj.mediaeval.evaluation.datasets.PPK2012ExtractCompare;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator.Mean;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.DoubleObjectPair;

import ch.akuhn.matrix.SparseMatrix;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SED2013SolrSimilarityMatrix {
	private static final class DoublePhotoPairComparator implements Comparator<DoubleObjectPair<IndexedPhoto>> {
		@Override
		public int compare(DoubleObjectPair<IndexedPhoto> o1, DoubleObjectPair<IndexedPhoto> o2) {
			return ((Long)o1.second.first).compareTo(o2.second.first);
		}
	}

	private static Logger logger = Logger.getLogger(SED2013SolrSimilarityMatrix.class);

	/**
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public static void main(String[] args) throws IOException, XMLStreamException {
		final SED2013Index index = SED2013Index.instance("http://localhost:8983/solr/sed2013_train");
//		final SED2013Index index = SED2013Index.instance();
		try{
			constructSimilarityMatrix(args, index);
		}finally{
			SED2013Index.shutdown();
		}
	}
	final static double eps = 0.1;
	final static int solrQueryN = 500;
	private static void constructSimilarityMatrix(String[] args, final SED2013Index index)
			throws IOException, XMLStreamException, FileNotFoundException {
		// Some choice experiments
		String bigFile = args[0];
		String expRoot = args[1];
		String tfidf = String.format("%s/training.sed2013.photo_tfidf",expRoot);
		String featurecache = String.format("%s/train.all.featurecache",expRoot);
		String matRoot = String.format("%s/training.sed2013.solr.matrixlib.allparts.%2.2f.%d.sparsematrix",expRoot,eps,solrQueryN);
		final File matOut = new File(matRoot);
		if(!matOut.exists()) matOut.mkdirs();
		SolrQuery q = new SolrQuery("*:*");
		q.setSortField("index", ORDER.asc);
		logger.info(String.format("Loading dataset: %s ", bigFile));

		final List<ExtractorComparator<Photo, ? extends FeatureVector>> fe = PPK2012ExtractCompare.similarity(tfidf, featurecache);


		SolrStream solrStream = new SolrStream(q, index.getSolrIndex());
		final int collectionN = solrStream.getNumResults();
		solrStream
		.map(new SolrDocumentToIndexedPhoto())
		.forEach(new Operation<IndexedPhoto>() {
			int numberOfImages = collectionN;
			@Override
			public void perform(IndexedPhoto p) {
				QueryResponse res;
				File rowOut = new File(matOut,String.format("%d",p.first));
				if(rowOut.exists()){
					return;
				}
				rowOut.mkdir();
				try {
					res = index.query(p.second, solrQueryN);
					SolrDocumentList results = res.getResults();


					final Mean<Photo> comp = new CombinedFVComparator.Mean<Photo>(fe) ;
					Map<String, SparseMatrix> rowmat = buildComparatorSparseRow(p, results, comp);
					for (Entry<String, SparseMatrix> namerow : rowmat.entrySet()) {
						String comparator = namerow.getKey();
						File compOut = new File(rowOut,String.format("%s.mat",comparator));
						SparseMatrix comprow = namerow.getValue();
						IOUtils.writeToFile(comprow, compOut);
						if(p.first%100 == 0){
							logger.debug(String.format("Working on Photo %s, index %d, Comparator: %s, sparcity: %2.5f",p.second.getId(),p.first,comparator,MatrixUtils.sparcity(comprow)));
						}
					}
				} catch (SolrServerException e) {
					logger.error("Error querying for photo: " + p.second.getId(),e);
				} catch (IOException e) {
					logger.error("Error writing photot to file: " + p.second.getId(),e);
				}

			}

			private Map<String,SparseMatrix> buildComparatorSparseRow(IndexedPhoto p, SolrDocumentList results, CombinedFVComparator<Photo> comp) {

				Map<String,TreeSet<DoubleObjectPair<IndexedPhoto>>> treemap = new HashMap<String,TreeSet<DoubleObjectPair<IndexedPhoto>>>();

				for (SolrDocument photoIndex : results) {
					IndexedPhoto ip = IndexedPhoto.fromDoc(photoIndex);
					Map<String, Double> compare = comp.compareAggregation(p.second, ip.second);
//					if(compare > eps){
//						tree.add(DoubleObjectPair.pair(compare, ip));
//					}
					for (Entry<String, Double> comparatorScore : compare.entrySet()) {
						TreeSet<DoubleObjectPair<IndexedPhoto>> tree = treemap.get(comparatorScore.getKey());
						if(tree == null) {
							treemap.put(comparatorScore.getKey(), tree = new TreeSet<DoubleObjectPair<IndexedPhoto>>(new DoublePhotoPairComparator()));
						}

						if(comparatorScore.getValue() > eps){
							tree.add(DoubleObjectPair.pair(comparatorScore.getValue(), ip));
						}

					}
				}
				Map<String, SparseMatrix> rowmats = new HashMap<String,SparseMatrix>();
				for (Entry<String, TreeSet<DoubleObjectPair<IndexedPhoto>>> nametree: treemap.entrySet()) {
					TreeSet<DoubleObjectPair<IndexedPhoto>> tree = nametree.getValue();
					SparseMatrix rowmat = new SparseMatrix(1, numberOfImages);
					for (DoubleObjectPair<IndexedPhoto> pair : tree) {
						rowmat.put(0, (int) pair.second.first, pair.first);
					}
					rowmats.put(nametree.getKey(), rowmat);
				}
				return rowmats;
			}
		});
	}
}
