package org.openimaj.mediaeval.sandbox;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.vis.general.HeatMap;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.Vector.Entry;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SparseMatrixMakeSymmetric {
	public static void main(String[] args) throws IOException {
		String inroot = "/home/ss/Experiments/mediaeval/SED2013/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/ALL/";
		String outroot = "/home/ss/Experiments/mediaeval/SED2013/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined.corrected/ALL/";
		File[] mats = new File(inroot).listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".mat");
			}
		});
		File out = new File(outroot);
		out.mkdirs();
		for (File file : mats) {
			File matout = new File(out,file.getName());
			System.out.println("Working on: " + file.getName());
			if(matout.exists()) {
				System.out.println("... Exists! Moving on!");
				continue;
			}
			System.out.println("... Loading");
			SparseMatrix sp = IOUtils.readFromFile(file);
			System.out.println("... making symmetric");
			sp = makeSymmetric(sp);
			System.out.println("... saving to: " + matout);
			IOUtils.writeToFile(sp, matout);
		}
//		
//		SparseMatrix sub = MatlibMatrixUtils.subMatrix(sp, 0, 1000, 0, 1000);
//		HeatMap hm = new HeatMap(800, 800);
//		hm.setData(sub.asArray());
//		hm.showWindow("Things");
		
	}

	private static SparseMatrix makeSymmetric(SparseMatrix sp) {
		System.out.println("Start density: " + (1-MatlibMatrixUtils.sparsity(sp)));
		int r = 0;
		SparseMatrix retmat = new SparseMatrix(sp.rowCount(),sp.columnCount());
		for (Vector row : sp.rows()) {
			for (Entry ent : row.entries()) {
				if(ent.value<0.6)continue;
				retmat.put(r, ent.index, ent.value);
				retmat.put(ent.index,r, ent.value);
			}
			r++;
		}
		System.out.println("End density: " + (1-MatlibMatrixUtils.sparsity(retmat)));
		return retmat;
	}
}
