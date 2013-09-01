package org.openimaj.mediaeval.evaluation.solr.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.kohsuke.args4j.Option;
import org.openimaj.mediaeval.data.util.SimilarityMatrixReader;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment;
import org.openimaj.util.pair.IntIntPair;
import org.openimaj.util.pair.Pair;

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
		name = "--random-start-mode",
		aliases = "-rsm",
		required = false,
		usage = "Start at random locations this many times, the random-start-length (rsl) must also be provided",
		metaVar = "INTEGER"
	)
	protected int randomStartMode = 0;
	
	@Option(
		name = "--random-start-range",
		aliases = "-rsr",
		required = false,
		usage = "The range within which random starts can be selected",
		metaVar = "INTEGER"
	)
	protected int randomStartRange = 100000;
	
	@Option(
		name = "--random-start-length",
		aliases = "-rsl",
		required = false,
		usage = "How many items to select given a random start mode count",
		metaVar = "INTEGER"
	)
	protected int randomStartLength = 100;
	
	@Option(
		name = "--random-start-seed",
		aliases = "-rss",
		required = false,
		usage = "If greater than 0, serves as a seed for the random start",
		metaVar = "INTEGER"
	)
	protected int randomStartSeed = -1;
	
	@Option(
		name = "--index",
		aliases = "-si",
		required = true,
		usage = "The lucene index used to query by the similarity matrix indecies to build the ground truth."
	)
	protected String index;
	
	@Option(
		name = "--simmat-cache",
		aliases = "-smcache",
		required = false,
		usage = "When the cache is set, an attempt is made to cache all the similarity matricies loaded (and the specific rows requested). This should NOT be used with huge matricies."
	)
	protected String simmatCache;

	protected Iterator<String> simMatIter;

	private List<IntIntPair> startEndList;

	private Iterator<IntIntPair> startEndListIter;

	
	/**
	 * prepare the experimental setups
	 */
	public void setup(){
		simMatIter = this.simmat.iterator();
		if(startEndListIter == null && randomStartMode > 0){
			Random random = new Random();
			if(this.randomStartSeed > 0){
				random = new Random(this.randomStartSeed);
			}
			this.startEndList = new ArrayList<IntIntPair>();
			for (int i = 0; i < randomStartMode; i++) {
				int rstart = random.nextInt(randomStartRange-this.randomStartLength);
				this.startEndList.add(IntIntPair.pair(rstart, rstart+this.randomStartLength));
			}
			this.startEndListIter = startEndList.iterator();
			IntIntPair startEnd = this.startEndListIter.next();
			this.start = startEnd.first;
			this.end= startEnd.second;
			
		}
	}
	
	/**
	 * @return whether there is another experiment setup
	 */
	abstract boolean hasNextSimmat();
	
	/**
	 * @return whether there is another experiment setup
	 */
	public boolean hasNextSimmatConfiguration(){
		return this.hasNextSimmat() || this.hasNextStartEnd();
	}
	
	private boolean hasNextStartEnd() {
		return startEndList!=null && startEndListIter.hasNext();
	}

	
	abstract NamedSolrSimilarityMatrixClustererExperiment nextSimmat();
	
	/**
	 * @return the next {@link SolrSimilarityMatrixClustererExperiment}
	 * 
	 */
	public NamedSolrSimilarityMatrixClustererExperiment nextSimmatConfiguration(){
		if(!this.hasNextSimmat()){
			IntIntPair startEnd = this.startEndListIter.next();
			this.start = startEnd.first;
			this.end = startEnd.second;
			this.setup();
		}
		return this.nextSimmat();
	}

	/**
	 * @param root
	 * @param desiredMatricies
	 * @return calles {@link SimilarityMatrixReader#readSparseMatricies(String, String...)} handling the {@link SimilarityMatrixWrapper} construction
	 * @throws IOException
	 */
	public Map<String,SimilarityMatrixWrapper> readSparseMatricies(String root,String ... desiredMatricies) throws IOException {
		Map<String, SimilarityMatrixWrapper> retMats = SimilarityMatrixReader.readCachedSparseMatricies(simmatCache,this.start,this.end,root, desiredMatricies);	
		return retMats;
	}
	
	
}
