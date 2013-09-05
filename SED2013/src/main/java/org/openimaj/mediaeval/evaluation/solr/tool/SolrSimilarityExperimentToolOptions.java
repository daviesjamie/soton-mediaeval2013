package org.openimaj.mediaeval.evaluation.solr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.mediaeval.evaluation.solr.tool.ExperimentSetupMode.NamedClusterer;
import org.openimaj.mediaeval.evaluation.solr.tool.SimMatSetupMode.NamedSolrSimilarityMatrixClustererExperiment;

/**
 *
 *	TODO: 
 *
 *READ THIS FIRST!!!!!
 *You need to:
 *	generate the test IMAGE similarity matricies
 *
 * party...
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SolrSimilarityExperimentToolOptions {
	private final static Logger logger = Logger.getLogger(SolrSimilarityExperimentTool.class);
	private String[] args;
	
	
	@Option(
		name = "--experiment-set-root",
		aliases = "-o",
		required = true,
		usage = "The root of the experiment. Modes will fill this with results"
	)
	private String root;
	
	@Option(
		name = "--force",
		aliases = "-f",
		required = false,
		usage = "Force the removal of existing experiemnts"
	)
	boolean force = false;
	
	@Option(
		name = "--no-logs",
		aliases = "-nl",
		required = false,
		usage = "Don't generate the log file"
	)
	boolean nologs = false;
	
	@Option(
		name = "--experiment-mode",
		aliases = "-em",
		required = true,
		usage = "The experiment mode",
		handler = ProxyOptionHandler.class
	)
	ExperimentSetupModeOption experimentSetupMode = null;
	ExperimentSetupMode experimentSetupModeOp = null;
	
	@Option(
		name = "--simmat-mode",
		aliases = "-smm",
		required = true,
		usage = "How the simmats are to be processed",
		handler = ProxyOptionHandler.class
	)
	SimMatModeOption simmatSetupMode = null;
	SimMatSetupMode simmatSetupModeOp = null;
	
	private Iterator<String> simMatIter;
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
		simmatSetupModeOp.setup();
		this.prepareNextSimmat();
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
		return experimentSetupModeOp.hasNextSetup() || this.simmatSetupModeOp.hasNextSimmatConfiguration();
	}

	/**
	 * @throws Exception 
	 * 
	 */
	public void performNextExperiment() throws Exception {
		PrintWriter reportWriter = null, correctWriter = null, estimatedWriter = null;
		try{			
			if(!experimentSetupModeOp.hasNextSetup()){
				prepareNextSimmat();
			}
			NamedClusterer namedClusterer = this.experimentSetupModeOp.nextClusterer();
			File setupDir= new File(this.experimentRoot,this.experimentSetupMode.name());
			setupDir= new File(setupDir,String.format("%d_%d",this.simmatSetupModeOp.start,this.simmatSetupModeOp.end));
			setupDir.mkdirs();
			correctWriter = new PrintWriter(new File(setupDir,"correct.txt")); // here we generate the "correct.txt", it should be identical for all the same star_end
			setupDir= new File(setupDir,namedClusterer.name);
			setupDir.mkdirs();
			this.experiment.setNextClusterer(namedClusterer.clusterer);
			File reportFile = new File(setupDir,"report.txt");
			if(!force && reportFile.exists()) {
				logger.info("Skipping. Experiement Exists: " + setupDir.getAbsolutePath());
				return;
			}
			estimatedWriter = new PrintWriter(new File(setupDir,"estimated.txt"));
			logger.info("Starting experiment: " + setupDir.getAbsolutePath());
			addSetupLogger(setupDir);
			ExperimentContext c = ExperimentRunner.runExperiment(this.experiment);
			removeSetupLogger(setupDir);
			this.experiment.writeIndexClusters(correctWriter, this.experiment.analysis.correct);
			this.experiment.writeIndexClusters(estimatedWriter, this.experiment.analysis.estimated);
			reportWriter = new PrintWriter(reportFile);
			reportWriter.println(c);
			reportWriter.flush();
			
		}finally{
			if(reportWriter != null) reportWriter.close();
			if(correctWriter != null) correctWriter.close();
			if(estimatedWriter != null) estimatedWriter.close();
		}
	}

	private void removeSetupLogger(File setupDir) {
		if(nologs )return;
		Appender app = Logger.getRootLogger().getAppender("setupAppender");
		app.close();
		Logger.getRootLogger().removeAppender("setupAppender");
		
	}

	private void addSetupLogger(File setupDir) {
		if(nologs )return;
		Appender anapp = (Appender) Logger.getRootLogger().getAllAppenders().nextElement();
		
		FileAppender app;
		try {
			String f = new File(setupDir.getAbsolutePath(),"log").getAbsolutePath();
			
			app = new FileAppender(anapp.getLayout(), f );
			app.setName("setupAppender");
			app.setThreshold(Level.DEBUG);
			Logger.getRootLogger().addAppender(app);
		} catch (IOException e) {
			logger.error("Could not add setup appender",e);
		}
	}

	private void prepareNextSimmat() {
		NamedSolrSimilarityMatrixClustererExperiment nextMode = this.simmatSetupModeOp.nextSimmatConfiguration();
		this.experiment = (SolrSimilarityExperimentTool) nextMode.exp;
		this.experimentRoot = new File(new File(this.root,this.simmatSetupMode.name()),nextMode.name);
		this.experimentSetupModeOp.setup();
	}

}
