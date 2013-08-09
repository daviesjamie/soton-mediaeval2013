package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;
import org.openimaj.ml.clustering.spectral.GraphLaplacian.Normalised;
import org.openimaj.ml.clustering.spectral.GraphLaplacian.Unnormalised;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.eigenvalues.FewEigenvalues;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SimilarityMatrixToMatlab {
	private static String expHome = "/home/ss/Experiments/mediaeval/SED2013/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/";

	public static void main(String[] args) throws IOException {
		SimilarityMatrixWrapper wrap = new SimilarityMatrixWrapper(expHome  + "/ALL/aggregationMean.mat", 0, 1000);
//		saveForPython(wrap);
		
		GraphLaplacian gl = new GraphLaplacian.Normalised();
		SparseMatrix L = gl.laplacian(wrap.matrix());
		FewEigenvalues fev = FewEigenvalues.of(L).greatest(40);
		fev.run();
		System.out.println(Arrays.toString(fev.value));
		
	}

	private static void saveForPython(SimilarityMatrixWrapper wrap)
			throws IOException {
		MLDouble matarr = MatlibMatrixUtils.asMatlab(wrap.matrix());
		ArrayList<MLArray> data = new ArrayList<MLArray>();
		data.add(matarr);
		new MatFileWriter(new File(expHome + "/1000/aggregationMean.matlab"), data);
	}
}
