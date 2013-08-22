package org.openimaj.mediaeval.evaluation.solr.tool;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SolrSimilarityExperimentToolOptions {

	private String[] args;
	@Option(
			name = "--start",
			aliases = "-s",
			required = false,
			usage = "The index in the similarity matrix to start with. Default is 0")
	private int start = 0;
	
	@Option(
			name = "--end",
			aliases = "-e",
			required = false,
			usage = "The index in the similarity matrix to end with. Default is -1 (i.e. all)")
	private int end = -1;
	
	@Option(
			name = "--index",
			aliases = "-si",
			required = true,
			usage = "The lucene index used to query by the similarity matrix indecies to build the ground truth.")
	private String index;
	
	@Option(
			name = "--simmat",
			aliases = "-sm",
			required = true,
			usage = "The solr index used to query by the similarity matrix indecies to build the ground truth.")
	private String simmat;

	/**
	 * @param args
	 */
	public SolrSimilarityExperimentToolOptions(String[] args) {
		this.args = args;
		prepare();
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

	/**
	 * @return the start index
	 */
	public int getStart() {
		return start;
	}
	
	/**
	 * @return the end index
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * @return the index
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * @return whether there is a next valid experiment
	 */
	public boolean hasNextExperiment() {
		return false;
	}

	/**
	 * 
	 */
	public void performNextExperiment() {
		
	}

}
