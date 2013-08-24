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
		System.setProperty("sed2013.solr.home", tool.input);
		SED2013SolrSimilarityMatrix simmat = new SED2013SolrSimilarityMatrix(tool.tfidf, tool.featurecache);
		
		Map<String, SparseMatrix> allmats = simmat.createSimilarityMatricies();
		simmat.write(tool.output, allmats);
	}
}
