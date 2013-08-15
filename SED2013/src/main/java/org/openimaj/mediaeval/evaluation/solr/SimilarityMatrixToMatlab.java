package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.openimaj.math.matrix.MatlibMatrixUtils;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SimilarityMatrixToMatlab {
	
//	private static String expRoot = "/home/ss/Experiments/mediaeval/SED2013";
	private static String expRoot = "/Users/ss/Experiments/sed2013";
	private static String expHome = expRoot  + "/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/";

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		SimilarityMatrixWrapper wrap = new SimilarityMatrixWrapper(expHome  + "/ALL/aggregationMean.mat", 0, 1000);
		saveForPython(wrap);	
	}

	private static void saveForPython(SimilarityMatrixWrapper wrap)
			throws IOException {
		MLDouble matarr = MatlibMatrixUtils.asMatlab(wrap.matrix());
		ArrayList<MLArray> data = new ArrayList<MLArray>();
		data.add(matarr);
		new MatFileWriter(new File(expHome + "/1000/aggregationMean.matlab"), data);
	}
}
