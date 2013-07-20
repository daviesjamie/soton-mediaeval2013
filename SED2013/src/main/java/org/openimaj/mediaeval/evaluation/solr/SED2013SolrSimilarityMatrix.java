package org.openimaj.mediaeval.evaluation.solr;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;
import gov.sandia.cognition.math.matrix.mtj.SparseVector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.openimaj.feature.FeatureVector;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.CFMatrixUtils;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.evaluation.datasets.PPK2012ExtractCompare;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator.Mean;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.util.function.Operation;

import com.aetrion.flickr.photos.Photo;

public class SED2013SolrSimilarityMatrix {
	private static Logger logger = Logger.getLogger(SED2013SolrSimilarityMatrix.class);

	public static void main(String[] args) throws IOException, XMLStreamException {
		final SED2013Index index = SED2013Index.instance("http://localhost:8983/solr/sed2013_train");
//		final SED2013Index index = SED2013Index.instance();
		try{
			constructSimilarityMatrix(args, index);
		}finally{
			SED2013Index.shutdown();
		}
	}

	private static void constructSimilarityMatrix(String[] args, final SED2013Index index)
			throws IOException, XMLStreamException, FileNotFoundException {
		// Some choice experiments
		String bigFile = args[0];
		String expRoot = args[1];
		String tfidf = String.format("%s/training.sed2013.photo_tfidf",expRoot);
		String featurecache = String.format("%s/train.all.featurecache",expRoot);
		String matRoot = String.format("%s/training.sed2013.solr.sparsematrix",expRoot);
		final int solrQueryN = 200;
		final double eps = 0.4;
		final int collectionN = 306144;

		final SparseMatrix mat = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(collectionN, collectionN);
		logger.info(String.format("Loading dataset: %s ", bigFile));
		File xmlFile = new File(bigFile);

		final List<ExtractorComparator<Photo, ? extends FeatureVector>> fe = PPK2012ExtractCompare.similarity(tfidf, featurecache);
		final File matOut = new File(matRoot);
		if(!matOut.exists()) matOut.mkdirs();
		new XMLCursorStream(xmlFile,"photo")
		.map(new CursorWrapperPhoto())
		.forEach(new Operation<Photo>() {
			int seen = 0;
			@Override
			public void perform(Photo p) {
				QueryResponse res;
				File rowOut = new File(matOut,String.format("%d.mat",seen));
				if(rowOut.exists()){
					seen++;
					return;
				}
				try {
					SparseMatrix rowmat = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(1, collectionN);
					SparseVector row = rowmat.getRow(0);
					res = index.query(p, solrQueryN);
					final Mean<Photo> comp = new CombinedFVComparator.Mean<Photo>(fe) ;
					for (SolrDocument photoIndex : res.getResults()) {
						IndexedPhoto ip = IndexedPhoto.fromDoc(photoIndex);
						double compare = comp.compare(p, ip.second);
						if(compare > eps){
							row.setElement((int) ip.first, compare);
						}
					}
					mat.setRow(seen, row);
					IOUtils.writeToFile(rowmat.getInternalMatrix(), rowOut);
					if(seen%100 == 0){
						logger.debug(String.format("Working on Photo %s, index %d, sparcity: %2.5f",p.getId(),seen,CFMatrixUtils.sparcity(rowmat)));
					}
				} catch (SolrServerException e) {
					logger.error("Error querying for photo: " + p.getId(),e);
				} catch (IOException e) {
					logger.error("Error writing photot to file: " + p.getId(),e);
				}
				seen++;

			}
		});
	}
}
