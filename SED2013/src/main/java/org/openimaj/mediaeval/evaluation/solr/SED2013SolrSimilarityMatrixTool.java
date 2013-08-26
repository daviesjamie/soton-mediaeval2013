package org.openimaj.mediaeval.evaluation.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ch.akuhn.matrix.SparseMatrix;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013SolrSimilarityMatrixTool {
	@Option(
		name = "--solr-index",
		aliases = "-i",
		required = true,
		usage = "The index from which to create a similarity matrix."
	)
	private String input;
	
	@Option(
		name = "--force-http",
		aliases = "-h",
		required = false,
		usage = "The provided solr index is treated as a URL to a solr server"
	)
	private boolean forceHTTP;
	
	@Option(
		name = "--tfidf-location",
		aliases = "-t",
		required = true,
		usage = "The location of the tfidf statistics file."
	)
	private String tfidf;
	
	@Option(
		name = "--featurecache-location",
		aliases = "-fc",
		required = true,
		usage = "The location of the feature cache."
	)
	private String featurecache;
	
	@Option(
		name = "--simmat-output",
		aliases = "-o",
		required = true,
		usage = "The root to save all the similarity matricies"
	)
	private String output;
	
	@Option(
		name = "--incremental-build",
		aliases = "-inc",
		required = false,
		usage = " if set to a positive number, split the final matrix as a number of matricies in a directory. Each matrix will be of the same dimensions, but will only contain inc completed rows."
	)
	private int incBuild = -1;
	
	@Option(
		name = "--solr-nsearch",
		aliases = "-sn",
		required = false,
		usage = " Number of results to allow per item."
	)
	private int solrN = 200;
	
	@Option(
		name = "--solr-eps",
		aliases = "-se",
		required = false,
		usage = " The score at which point to set a similarity to non zero"
	)
	private double solrEps = 0.4d;
	
	@Option(
		name = "--no-solr-sort",
		aliases = "-nosort",
		required = false,
		usage = "Do Not Sort the solr results by their distance in time and geo"
	)
	private boolean deactivateSort = false;

	private String[] args;

	public SED2013SolrSimilarityMatrixTool(String[] args) {
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
	
	public static void main(String[] args) throws IOException, XMLStreamException {
		SED2013SolrSimilarityMatrixTool tool = new SED2013SolrSimilarityMatrixTool(args);
		SED2013SolrSimilarityMatrix simmat = null;
		if(tool.forceHTTP)
		{
			simmat = new SED2013SolrSimilarityMatrix(tool.tfidf, tool.featurecache, tool.input);
		}
		else{
			System.setProperty("sed2013.solr.home", tool.input);
			simmat = new SED2013SolrSimilarityMatrix(tool.tfidf, tool.featurecache, tool.input);
		}
		
		simmat.eps = tool.solrEps;
		simmat.solrQueryN = tool.solrN;
		simmat.deactivateSort = tool.deactivateSort;
		
		if(tool.incBuild < 0){			
			Map<String, SparseMatrix> allmats = simmat.createSimilarityMatricies();
			simmat.write(tool.output, allmats);
		}
		else{
			simmat.createAndWrite(tool.output,tool.incBuild);
		}
	}
}
