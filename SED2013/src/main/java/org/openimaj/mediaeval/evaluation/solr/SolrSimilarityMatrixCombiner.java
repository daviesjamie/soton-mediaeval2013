package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.openimaj.io.IOUtils;
import org.openimaj.logger.LoggerUtils;
import org.openimaj.time.Timer;

import ch.akuhn.matrix.SparseMatrix;


/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SolrSimilarityMatrixCombiner {
	static Logger logger = Logger.getLogger(SolrSimilarityMatrixCombiner.class);
	public static void main(String[] args) throws IOException {
		String combinedOut = args[1];
		String combinedName = "ALL";
		if(args.length==3){
			int readN = Integer.parseInt(args[2]);
			combinedName = readN + "";
		}
		HashMap<String,SparseMatrix> mats = null;
		if(new File(combinedOut,combinedName).exists()){
			mats = readCombined(args);
		}
		else{
			mats = combineAndWrite(args);
		}
	}
	private static HashMap<String, SparseMatrix> readCombined(String[] args) throws IOException {
		String combinedOut = args[1];
		String combinedName = "ALL";
		if(args.length==3){
			int readN = Integer.parseInt(args[2]);
			combinedName = readN + "";
		}
		HashMap<String, SparseMatrix> mats = new HashMap<String, SparseMatrix>();
		File combDir = new File(combinedOut,combinedName);
		logger.debug("Loading files from: " + combDir);
		for (File featureRowFile : combDir.listFiles()) {
			String featureName = featureRowFile.getName();
			featureName = featureName.substring(0, featureName.length()-4);
			mats.put(featureName, (SparseMatrix) IOUtils.readFromFile(featureRowFile));
		}
		return mats;
	}
	private static HashMap<String, SparseMatrix> combineAndWrite(String[] args) throws IOException {
		String matRoot = args[0];
		String combinedOut = args[1];
		logger.debug("Listing files in: " + matRoot);
		File[] rowFiles = new File(matRoot).listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() && pathname.getName().matches("[0-9]+");
			}
		});

		String[] featureMatFiles = rowFiles[0].list();
		logger.debug(String.format("Found %d features",featureMatFiles.length));

		int readN = rowFiles.length;
		String combinedName = "ALL";
		if(args.length==3){
			readN = Integer.parseInt(args[2]);
			combinedName = readN + "";
		}
		logger.debug(String.format("Found %d files, sorting...",rowFiles.length));
		Arrays.sort(rowFiles,new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				Integer i2 = Integer.parseInt(o2.getName());
				Integer i1 = Integer.parseInt(o1.getName());
				return i1.compareTo(i2);
			}
		});
		logger.debug(String.format("Combining rows"));
		Timer t = Timer.timer();
		for (String featureFileName : featureMatFiles) {
			logger.debug(String.format("Started combination for " + featureFileName));
			SparseMatrix featureMat = new SparseMatrix(rowFiles.length, rowFiles.length);
			String featureOutName = featureFileName.substring(0, featureFileName.length()-4);
			for (File rowFile : rowFiles) {
				if(rowFile.list().length == 0) continue;
				int row = Integer.parseInt(rowFile.getName());
				SparseMatrix featureRowMat = IOUtils.readFromFile(new File(rowFile,featureFileName));
				featureMat.addToRow(row, featureRowMat.row(0));
				double taken = t.duration()/1000./60.;
				double remain = (taken/(row+1)) * (readN - row - 1) ;
				LoggerUtils.debug(logger, String.format("Reading row: %d, Took: %2.5fm, Expected: %2.5fm",row,taken, remain), row%1000==0);
				if(row >= readN) break;
			}
			logger.debug("Writing the combined matrix");
			File combDir = new File(combinedOut,combinedName);
			combDir.mkdirs();
			File featureOut =  new File(combDir,String.format("%s.mat",featureOutName));
			IOUtils.writeToFile(featureMat,featureOut);
		}
		return readCombined(args);
	}
}
