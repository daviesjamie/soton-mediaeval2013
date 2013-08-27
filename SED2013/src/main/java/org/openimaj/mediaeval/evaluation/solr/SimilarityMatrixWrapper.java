package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.Vector.Entry;

/**
 * Holds a similarity matrix loaded from a file. The similarity matrix can be a submatrix of the one 
 * loaded from the file
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SimilarityMatrixWrapper {
	private static final Logger logger = Logger.getLogger(SimilarityMatrixWrapper.class);
	private SparseMatrix similarityMatrix;
	private int end;
	private int start;
	
	private double[] minmaxmean;

	/**
	 * @param simMatrixFile from a file
	 * @param s the start location
	 * @param e the end. if -1 end SparseMatrix
	 * @throws IOException 
	 */
	public SimilarityMatrixWrapper(String simMatrixFile, int s, int e) throws IOException {
		this.end = e;
		this.start = s;
		logger .debug("Loading sparse matrix");
		SparseMatrix mat;
		mat = IOUtils.readFromFile(new File(simMatrixFile));
		if(end < 0) end = mat.columnCount() + end + 1;
		
		logger.debug("Extracting submatrix");
		this.similarityMatrix = MatlibMatrixUtils.subMatrix(mat,start,end,start,end);
		logger.debug(String.format("Submatrix dims: %d x %d" ,similarityMatrix.rowCount(),similarityMatrix.columnCount()));
		confirmMatrixIntegrity();
	}
	
	/**
	 * @param simMatrix the sim matrix
	 * @param s the start location
	 * @param e the end. if -1 end SparseMatrix
	 */
	public SimilarityMatrixWrapper(SparseMatrix simMatrix, int s, int e) {
		this.end = e;
		this.start = s;
		logger .debug("Loading sparse matrix");
		SparseMatrix mat = simMatrix;
		if(end < 0) end = mat.columnCount() + end + 1;
		
		logger.debug("Extracting submatrix");
		this.similarityMatrix = MatlibMatrixUtils.subMatrix(mat,start,end,start,end);
		logger.debug(String.format("Submatrix dims: %d x %d" ,similarityMatrix.rowCount(),similarityMatrix.columnCount()));
		confirmMatrixIntegrity();
	}
	
	private void confirmMatrixIntegrity() {
		int i = 0;
		for (Vector v : this.similarityMatrix.rows()) {
			v.put(i, 1);
			for (Entry ent : v.entries()) {
				if(Double.isNaN(ent.value)) throw new RuntimeException("NaN in matrix!");
			}
			i++;
		}
	}

	@Override
	public String toString() {
		meanmax();
		return String.format("Similarity Matrix. rxc = %d.\n min: %2.5f, max: %2.5f, mean: %2.5f",this.similarityMatrix.rowCount(),minmaxmean[0],minmaxmean[1],minmaxmean[2]);
	}

	private void meanmax() {
		if(this.minmaxmean!=null) return;
		double max = -Double.MAX_VALUE;
		double min = Double.MAX_VALUE;
		double mean = 0;
		int count = 0;
		for (Vector row : similarityMatrix.rows()) {
			for (Entry ent : row.entries()) {
				max = Math.max(max, ent.value);
				min = Math.min(min, ent.value);
				mean += ent.value ;
				count++;
			}
		}
		mean/=count;
		
		this.minmaxmean = new double[]{min,max,mean};
	}

	/**
	 * @return the underlying matrix
	 */
	public SparseMatrix matrix() {
		return this.similarityMatrix;
	}

	/**
	 * @return start index
	 */
	public int start() {
		return start;
	}

	/**
	 * @return the end index
	 */
	public int end() {
		return end;
	}
}
