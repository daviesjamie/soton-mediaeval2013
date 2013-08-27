package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment;

/**
 * A mode dealing with similarity matricies
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public abstract class SimMatSetupMode {
	protected class NamedSolrSimilarityMatrixClustererExperiment {
		String name;
		SolrSimilarityMatrixClustererExperiment exp;
	}
	@Option(
		name = "--simmat",
		aliases = "-sm",
		required = true,
		usage = "The similarity matrix to use for this experiment.",
		multiValued=true
	)
	protected List<String> simmat;
	
	@Option(
		name = "--simmat-root",
		aliases = "-smr",
		required = false,
		usage = "If this similarity matrix root is set the -sm provided are treated as mats within this root.",
		multiValued=true
	)
	protected String simmatRoot;
	
	@Option(
		name = "--start",
		aliases = "-s",
		required = false,
		usage = "The index in the similarity matrix to start with. Default is 0"
	)
	protected int start = 0;
	
	@Option(
		name = "--end",
		aliases = "-e",
		required = false,
		usage = "The index in the similarity matrix to end with. Default is -1 (i.e. all)"
	)
	protected int end = -1;
	
	@Option(
		name = "--index",
		aliases = "-si",
		required = true,
		usage = "The lucene index used to query by the similarity matrix indecies to build the ground truth."
	)
	protected String index;

	protected Iterator<String> simMatIter;
	
	/**
	 * prepare the experimental setups
	 */
	public void setup(){
		simMatIter = this.simmat.iterator();
	}
	
	/**
	 * @return whether there is another experiment setup
	 */
	public abstract boolean hasNextSimmat();
	
	/**
	 * @return the next {@link SolrSimilarityMatrixClustererExperiment}
	 * 
	 */
	public abstract NamedSolrSimilarityMatrixClustererExperiment nextSimmat();
}
