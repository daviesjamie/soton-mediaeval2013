package org.openimaj.mediaeval.evaluation.solr.tool;


/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class OneAtATimeSimMatMode extends SimMatSetupMode {

	
	private String simMatFile;

	@Override
	public boolean hasNextSimmat() {
		return simMatIter.hasNext();
	}

	@Override
	public NamedSolrSimilarityMatrixClustererExperiment nextSimmat() {
		NamedSolrSimilarityMatrixClustererExperiment ret = new NamedSolrSimilarityMatrixClustererExperiment();
		simMatFile = this.simMatIter.next();
		if(this.simmatRoot==null){		
			ret.exp = new SolrSimilarityExperimentTool(simMatFile, this.index, start, end);
		}
		else{
			ret.exp = new SolrSimilarityExperimentTool(simmatRoot,simMatFile, this.index, start, end);
		}
		ret.name = simMatFileName();
		
		return ret;
	}
	
	private String simMatFileName() {
		String[] split = this.simMatFile.split("/");
		String name = split[split.length-1];
		return name;
	}
}
