package org.openimaj.mediaeval.evaluation.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;

public class SimilarityMatrixInformationTool {

	
	@Option(
		name = "--input-matrix",
		aliases = "-i",
		required = true,
		usage = "The location of the matrix, either multi part, single part multi matrix or a single .mat"
	)
	private String matloc;
	
	@Option(
			name = "--matrix-name",
			aliases = "-m",
			required = false,
			usage = "Names of matricies",
			multiValued=true
		)
	private List<String> matnames = null;
	private String[] args;
	
	public SimilarityMatrixInformationTool(String[] args) {
		this.args = args;
		this.prepare();
	}

	private void prepare() {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java ... [options...] ");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	

	private void printMatStats(SparseMatrix value) {
		double used = value.used();
		long size = (long)value.rowCount() * value.columnCount();
		System.out.printf("Size: %dx%d (%d)\n", value.rowCount(),value.columnCount(),size);
		System.out.println("Sparcity: " + (used/size));
		double[] mmm = SimilarityMatrixWrapper.meanmaxmin(value);
		System.out.printf("Min/Max/Mean: %2.2f,%2.2f,%2.2f\n", mmm[0],mmm[1],mmm[2]);
		System.out.printf("Empty Rows: %d\n", countEmptyRows(value));
		
	}

	private int countEmptyRows(SparseMatrix value) {
		int emptyRow = 0;
		int r = 0;
		for (Vector row: value.rows()) {
			if(row.used() == 0) emptyRow++;
			r++;
		}
		return emptyRow;
	}

	private void printMatTitle(String key) {
		System.out.println(repeat("=",key.length()));
		System.out.println(key);
		System.out.println(repeat("=",key.length()));
	}
	private static String repeat(String string, int length) {
		String ret = "";
		for (int i = 0; i < length; i++) {
			ret += string;
		}
		return ret;
	}

	public static void main(String[] args) throws IOException {
		SimilarityMatrixInformationTool tool = new SimilarityMatrixInformationTool(args);
		Map<String, SparseMatrix> mats = null;
		if(tool.matnames == null || tool.matnames.size() == 0){			
			mats = SED2013SolrSimilarityMatrix.readSparseMatricies(tool.matloc);
		}
		else{
			mats = SED2013SolrSimilarityMatrix.readSparseMatricies(tool.matloc,tool.matnames.toArray(new String[tool.matnames.size()]));
		}
		for (Entry<String, SparseMatrix> ent : mats.entrySet()) {
			tool.printMatTitle(ent.getKey());
			tool.printMatStats(ent.getValue());
			System.out.println(repeat("=",ent.getKey().length()));
		}
	}
}
