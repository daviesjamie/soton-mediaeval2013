package org.openimaj.mediaeval.sandbox;

import java.io.File;
import java.io.IOException;

import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.vis.general.HeatMap;

import ch.akuhn.matrix.SparseMatrix;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SparseMatrixTestVis {
	
	public static void main(String[] args) throws IOException {
		SparseMatrix sp = IOUtils.readFromFile(new File("/home/ss/Experiments/mediaeval/SED2013/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/ALL/aggregationMean.mat"));
		
		SparseMatrix sub = MatlibMatrixUtils.subMatrix(sp, 0, 1000, 0, 1000);
		
		HeatMap hm = new HeatMap(800, 800);
		hm.setData(sub.asArray());
		hm.showWindow("Aggr");
	}

}
