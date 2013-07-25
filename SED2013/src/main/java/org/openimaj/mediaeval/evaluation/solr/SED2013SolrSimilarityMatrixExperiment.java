package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
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
import org.openimaj.logger.LoggerUtils;
import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.mediaeval.data.SolrDocumentToIndexedPhoto;
import org.openimaj.mediaeval.data.SolrStream;
import org.openimaj.mediaeval.evaluation.datasets.PPK2012ExtractCompare;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator.Mean;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.time.Timer;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.DoubleObjectPair;

import scala.actors.threadpool.Arrays;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.SparseVector;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SED2013SolrSimilarityMatrixExperiment {

	private static final class MatFileFilter implements
			FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return !name.endsWith("[.]mat");
		}
	}
	private static Logger logger = Logger.getLogger(SED2013SolrSimilarityMatrixExperiment.class);
	private static Timer t;

	/**
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public static void main(String[] args) throws IOException, XMLStreamException {
		String expRoot = args[0];
		String similarityMatrix = args[1];
		
		String matRoot = String.format("%s/%s",expRoot,similarityMatrix);
		logger.debug("Mat File Root: " + matRoot);
		File matrixRoot = new File(matRoot);
		if(!matrixRoot.exists()){
			logger.error("Matrix Root does not exist");
			return;
		}
		logger.debug("Counting mat files");
		String[] matFiles = matFileList(matrixRoot);
		int countFiles = matFiles.length;
		String outNameAppend = "ALL";
		if(args.length == 3){
			countFiles = Integer.parseInt(args[2]);
			outNameAppend = "" + countFiles;
		}
		
		File outRoot = new File(String.format("%s/%s.combined",expRoot,similarityMatrix));
		if(!outRoot.exists()) outRoot.mkdirs();
		File outFile = new File(outRoot,String.format("%s.%s.mat",similarityMatrix,outNameAppend));
		SparseMatrix mat = null;
		t = Timer.timer();
		if(!outFile.exists())
		{
			mat = loadMatFiles(countFiles,matFiles, matrixRoot);
			logger.debug("Writing output to: " + outFile);
			IOUtils.writeToFile(mat, outFile);
		}
		else{
			mat = IOUtils.readFromFile(outFile); 
		}
		
		logger.debug(String.format("Mat file loaded with %d dimensions, took %2.2fm",mat.columnCount(),t.duration()/1000./60.));
		logger.debug(String.format("Loaded with sparcity: %2.5f",MatrixUtils.sparcity(mat)));
	}
	private static SparseMatrix loadMatFiles(int countFiles, String[] matFiles, File matrixRoot) throws IOException {
		SparseMatrix mat = new SparseMatrix(countFiles,countFiles);
		logger.debug("Found mat files: " + countFiles);
		logger.debug("Creating sparse matrix");
		
		for (int i = 0; i < countFiles; i++) {
			String matFile = matFiles[i];
			int rowi = rowFromFilename(matFile);
			SparseMatrix row = IOUtils.readFromFile(new File(matrixRoot,matFile));
//			if(countFiles==matFiles.length)
//				mat.setRow(rowi,(SparseVector) row.row(0));
//			else
//			{
				for (ch.akuhn.matrix.Vector.Entry ent: row.row(0).entries()) {
					if(ent.index<countFiles){
						mat.put(rowi, ent.index, ent.value);
					}
				}
//			}
			long taken = t.duration()/1000;
			long remaining = (long) (((taken / (double)(i+1)) * countFiles) - taken)/60; 
			LoggerUtils.debug(logger,String.format("%d files read in %dm, remaining: %dm",i,taken/60,remaining),(i)%1000==0);
		}
		return mat;
	}
	private static Integer rowFromFilename(String matFile) {
		return Integer.parseInt(matFile.split("[.]")[0]);
	}
	private static String[] matFileList(File matrixRoot) {
		String[] ret = matrixRoot.list(new MatFileFilter());
		Arrays.sort(ret, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return rowFromFilename(o1).compareTo(rowFromFilename(o2));
			}
		});
		return ret;
	}
}
