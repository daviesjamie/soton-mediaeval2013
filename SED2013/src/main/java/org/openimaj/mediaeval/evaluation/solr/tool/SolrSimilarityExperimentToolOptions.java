package org.openimaj.mediaeval.evaluation.solr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.mediaeval.evaluation.solr.tool.ExperimentSetupMode.NamedClusterer;

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
		usage = "The index in the similarity matrix to start with. Default is 0"
	)
	private int start = 0;
	
	@Option(
		name = "--end",
		aliases = "-e",
		required = false,
		usage = "The index in the similarity matrix to end with. Default is -1 (i.e. all)"
	)
	private int end = -1;
	
	@Option(
		name = "--index",
		aliases = "-si",
		required = true,
		usage = "The lucene index used to query by the similarity matrix indecies to build the ground truth."
	)
	private String index;
	
	@Option(
		name = "--simmat",
		aliases = "-sm",
		required = true,
		usage = "The similarity matrix to use for this experiment.",
		multiValued=true
	)
	private List<String> simmat;
	
	@Option(
		name = "--experiment-set-root",
		aliases = "-o",
		required = true,
		usage = "The root of the experiment. Modes will fill this with results"
	)
	private String root;
	
	@Option(
		name = "--experiment-mode",
		aliases = "-em",
		required = true,
		usage = "The experiment mode",
		handler = ProxyOptionHandler.class
	)
	ExperimentSetupModeOption experimentSetupMode = null;
	ExperimentSetupMode experimentSetupModeOp = null;
	private Iterator<String> simMatIter;
	private String simMatFile;
	private SolrSimilarityExperimentTool experiment;
	private File experimentRoot;

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
			setup();
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java ... [options...] ");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}

	private void setup() {
		simMatIter = this.simmat.iterator();
		this.prepareNextSimmat();
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
	 * @return experiment root
	 */
	public String getExperimentRoot(){
		return root;
	}

	/**
	 * @return whether there is a next valid experiment
	 */
	public boolean hasNextExperiment() {
		return experimentSetupModeOp.hasNextSetup() || simMatIter.hasNext();
	}

	/**
	 * @throws Exception 
	 * 
	 */
	public void performNextExperiment() throws Exception {
		if(!experimentSetupModeOp.hasNextSetup()){
			prepareNextSimmat();
		}
		NamedClusterer namedClusterer = this.experimentSetupModeOp.nextClusterer();
		File setupDir= new File(this.experimentRoot,this.experimentSetupMode.name());
		setupDir= new File(setupDir,namedClusterer.name);
		setupDir.mkdirs();
		this.experiment.setNextClusterer(namedClusterer.clusterer);
		PrintWriter reportWriter = new PrintWriter(new File(setupDir,"report.txt"));
		PrintWriter correctWriter = new PrintWriter(new File(setupDir,"correct.txt"));
		PrintWriter estimatedWriter = new PrintWriter(new File(setupDir,"estimated.txt"));
		ExperimentContext c = ExperimentRunner.runExperiment(this.experiment);
		this.experiment.writeIndexClusters(correctWriter, this.experiment.analysis.correct);
		this.experiment.writeIndexClusters(estimatedWriter, this.experiment.analysis.estimated);
		reportWriter.println(c);
		reportWriter.flush();
		reportWriter.close();
	}

	private void prepareNextSimmat() {
		simMatFile = this.simMatIter.next();
		this.experiment = new SolrSimilarityExperimentTool(simMatFile, this.index, start, end);
		this.experimentRoot = new File(this.root,simMatFileName());
		this.experimentSetupModeOp.setup();
	}

	private String simMatFileName() {
		String[] split = this.simMatFile.split("/");
		String name = split[split.length-1];
		return name;
	}

}
