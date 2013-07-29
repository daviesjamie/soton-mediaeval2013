package org.openimaj.mediaeval.evaluation.datasets;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.evaluation.cluster.ClusterEvaluator;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEClusterAnalyser;
import org.openimaj.mediaeval.evaluation.cluster.processor.SimilarityMatrixDoubleDBSCANWrapper;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne.Training;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.util.stream.Stream;

import scala.actors.threadpool.Arrays;
import ch.akuhn.matrix.SparseMatrix;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013SimilarityMatrixExperiment {
	private static Logger logger = Logger.getLogger(SED2013SimilarityMatrixExperiment.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String matrixLocation = "/Volumes/data/mediaeval/mediaeval-SED2013/tools/simmat/train.all.sparse";

		DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf =
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(
				1, 0.6, 2, new DoubleNearestNeighboursExact.Factory()
			);
		DoubleDBSCAN dbsConf = new DoubleDBSCAN(conf);
		Training ds = loadGroundtruth();
		SparseMatrix simMatrix = loadSparseMatrixByRows(matrixLocation,ds.numInstances(),conf);
		ClusterEvaluator<Photo, MEAnalysis> eval =
			new ClusterEvaluator<Photo, MEAnalysis>(
				new SimilarityMatrixDoubleDBSCANWrapper(simMatrix,dbsConf),
				new MEClusterAnalyser(),
				ds
			);
		int[][] evaluate = eval.evaluate();
		MEAnalysis res = eval.analyse(evaluate);

		System.out.println(res.getSummaryReport());
	}

	private static SparseMatrix loadSparseMatrixByRows(String matrixLocation, int numInstances, DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf) throws IOException {
		SparseMatrix ret = new SparseMatrix(numInstances, numInstances);
		logger.debug("Listing mat files in: " + matrixLocation);
		File[] rowFiles = new File(matrixLocation).listFiles(new java.io.FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith("mat");
			}
		});
		logger.debug("Sorting files by row index");
		Arrays.sort(rowFiles, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return matFileIndex(o1).compareTo(matFileIndex(o2));
			}
		});
//		for (File file : rowFiles) {
//			int i = matFileIndex(file);
//			if(i%100 == 0){
//				logger.debug("Loading row:" + i);
//			}
//			SparseMatrix row = IOUtils.readFromFile(file);
//			int j = 0;
//			for (Entry matrixEntry : row.row(0).entries()) {
//				double value = matrixEntry.value;
//				if(value > conf.getEps()){
//					ret.setElement(i, j, value);
//					ret.setElement(j, i, value);
//				}
//				j++;
//			}
//		}
		return ret;
	}

	private static Integer matFileIndex(File file) {
		String[] nameparts = file.getName().split("[.]");
		int i = Integer.parseInt(nameparts[0]);
		return i;
	}

	private static Training loadGroundtruth() throws Exception {
		// Some choice experiments
		String bigFile = "/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml";
		logger.info(String.format("Loading dataset: %s ", bigFile));
		File xmlFile = new File(bigFile);

		Stream<Photo> photoStream = new XMLCursorStream(xmlFile,"photo")
		.map(new CursorWrapperPhoto());

		InputStream clStream = new FileInputStream("/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train_gs.csv");
		Training dataset = new Training(clStream, photoStream);
		return dataset;
	}
}
