package org.openimaj.mediaeval.evaluation.solr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.mortbay.io.RuntimeIOException;
import org.openimaj.data.DoubleRange;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.tool.SpatialClustererSetupMode.NamedSpecClusterConf;
import org.openimaj.ml.clustering.spectral.AbsoluteValueEigenChooser;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.eigenvalues.Eigenvalues;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SpectralSetupMode extends ExperimentSetupMode{
	Logger logger = Logger.getLogger(SpectralSetupMode.class);
	/**
	 * The absolute eigen value change 
	 */
	@Option(
		name="--eig-start", 
		aliases="-eigs", 
		required=false, 
		usage="The eigenvalue proportion change used to choose eigen vectors. Start", 
		metaVar="DOUBLE"
	)
	public double eigStart = 0.2;
	
	/**
	 * The end eps to searc
	 */
	@Option(
		name="--eig-end", 
		aliases="-eige", 
		required=false, 
		usage="The eigenvalue proportion change used to choose eigen vectors. End", 
		metaVar="DOUBLE"
	)
	public double eigEnd = 0.9;
	
	/**
	 * The delta of eps to search
	 */
	@Option(
		name="--eig-delta", 
		aliases="-eigd", 
		required=false, 
		usage="The eigenvalue proportion change used to choose eigen vectors. Delta", 
		metaVar="DOUBLE"
	)
	public double eigDelta= 0.4;
	
	@Option(
		name = "--eig",
		aliases = "-eig",
		required = false,
		usage = "List of eig props to search",
		multiValued=true
	)
	private List<Double> eig = new ArrayList<Double>();
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--eig-select", 
		aliases="-eigsel", 
		required=false, 
		usage="The number of eigen values to ask for (proportioned of the total available)", 
		metaVar="DOUBLE"
	)
	public double eigsel = 0.05;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--eig-skip", 
		aliases="-eigskip", 
		required=false, 
		usage="The number of eigen values to ask for (proportioned of the total available)", 
		metaVar="INTEGER"
	)
	public int eigskip = 0;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--eig-value-scale", 
		aliases="-eigvscale", 
		required=false, 
		usage="Scale the selected eigen vectors by the sqrt of their eigen values. Mostly this should be == 1 but if selecting relatively small but important eigen vectors i might matter!", 
		metaVar="BOOLEAN"
	)
	public boolean eigvscale = false;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--save-as-matlab", 
		aliases="-matsave", 
		required=false, 
		usage="Save the Laplacian and Adjacency matricies as python matricies, for debugging!", 
		metaVar="STRING"
	)
	String pythonRoot = null;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--eigen-cache", 
		aliases="-eigcache", 
		required=false, 
		usage="Save the Laplacian and Adjacency matricies as python matricies, for debugging!", 
		metaVar="STRING"
	)
	String eigCache = null;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--force-spectral-threshold", 
		aliases="-specthresh", 
		required=false, 
		usage="Force a thresholding applied directly to the data array", 
		metaVar="DOUBLE"
	)
	Double thresh = null;
	boolean forceEigenCacheRefresh = true;


	private Iterator<Double> eigIter;

	@Option(
		name = "--spectral-clustering-mode",
		aliases = "-scm",
		required = true,
		usage = "The experiment mode used to cluster spectral clustering",
		handler = ProxyOptionHandler.class
	)
	SpatialClustererSetupOption experimentSetupMode = null;
	SpatialClustererSetupMode experimentSetupModeOp = null;
	
	
	enum GraphLaplacianMode{
		UNNORMALISED{
			@Override
			public GraphLaplacian lap() {
				return new GraphLaplacian.Unnormalised();
			}
			
		}, 
		NORMALISED{
			@Override
			public GraphLaplacian lap() {
				return new GraphLaplacian.Normalised();
			}
		}, 
		WARPED{
			@Override
			public GraphLaplacian lap() {
				return new GraphLaplacian.Warped();
			}
		};
		
		public abstract GraphLaplacian lap();
	}
	
	// The only GraphLaplacian that actually works is the normalised one, don't let the other modes get used!
//	@Option(
//		name = "--spectral-clustering-graph-laplacian",
//		aliases = "-scgl",
//		required = true,
//		usage = "The grpah laplacian used in spectral clustering",
//		handler = ProxyOptionHandler.class
//	)
	GraphLaplacianMode glMode = GraphLaplacianMode.NORMALISED;

	
	double currentEig = -1;
	private File eigcachefile;
	@Override
	public void setup() {
		currentEig = -1;
		this.experimentSetupModeOp.setup();
		if(this.eig.size() == 0){
			this.eigIter = new DoubleRange(eigStart, eigDelta, eigEnd).iterator();
		}
		else{
			this.eigIter = eig.iterator();
		}
		forceEigenCacheRefresh = true; // When the eigen
		if(eigCache!=null){			
			File eigdir = new File(eigCache);
			eigdir.mkdirs();
			eigcachefile = new File(eigdir,"spectralEigenvalues.dat");
		}
	}
	@Override
	public boolean hasNextSetup() {
		boolean innerExpHasNext = experimentSetupModeOp.hasNextSetup();
		boolean eigValsHasNext = this.eigIter.hasNext();
		return innerExpHasNext || eigValsHasNext;
	}
	
	@Override
	public NamedClusterer nextClusterer() {
		if(!experimentSetupModeOp.hasNextSetup() || this.currentEig == -1){
			experimentSetupModeOp.setup();
			this.currentEig = this.eigIter.next();
		}
		NamedSpecClusterConf nextClusterer = experimentSetupModeOp.nextClusterer();		
		NamedClusterer ret = new NamedClusterer();
		nextClusterer.conf.eigenChooser = new AbsoluteValueEigenChooser(currentEig, eigsel);
		nextClusterer.conf.laplacian = glMode.lap();
		nextClusterer.conf.skipEigenVectors = this.eigskip;
		nextClusterer.conf.eigenValueScale = eigvscale ;
		ret.clusterer = new DoubleSpectralClustering(nextClusterer.conf){ 
			@Override
			protected Eigenvalues spectralCluster(SparseMatrix data) {
				saveToPython("W", data);
				if(thresh != null){
					logger.info("Thresholding and binarizing the data with: " + thresh);
					data = MatlibMatrixUtils.threshold(data,thresh);
					saveToPython("Wthresh", data);
				}
				Eigenvalues eigret = null;
				if(forceEigenCacheRefresh || eigCache == null){
					logger.info("Doing the eigen decomposition (hold on to your butts)");
					eigret = super.spectralCluster(data);
					forceEigenCacheRefresh = false;
					if(eigCache!=null){
						logger.info("Writing eigenvalues to cache: " + eigcachefile);
						try{
							IOUtils.writeToFile(eigret,eigcachefile);
						} catch(Exception e){
							throw new RuntimeException(e);
						}
						
					}
				}
				else{
					// Try to load from the cache!
					logger.info("Loading eigenvalues from cache: " + eigcachefile);
					
					try{
						eigret = IOUtils.readFromFile(eigcachefile);
					} catch(Exception e){
						throw new RuntimeException(e);
					}
				}
				
				
				return eigret;
			}
			
			@Override
			protected SparseMatrix laplacian(SparseMatrix data) {
				SparseMatrix laplacian = super.laplacian(data);
				saveToPython("L", laplacian);
				return laplacian;
			}
			public void saveToPython(String name, SparseMatrix mat){
				if(pythonRoot==null)return;
				new File(pythonRoot).mkdirs();
				MLDouble ml = MatlibMatrixUtils.asMatlab(mat);
				ml.name = name;
				Collection<MLArray> cols = new ArrayList<MLArray>();
				cols.add(ml);
				try {
					new MatFileWriter(new File(pythonRoot,name + ".mat"), cols);
				} catch (IOException e) {
					throw new RuntimeIOException(e);
				}
			}
		};
		ret.name = String.format("eigvscale=%s/eigsel=%2.2f/eigskip=%d/eiggap=%2.2f/%s/%s",eigvscale,this.eigsel,this.eigskip,this.currentEig,this.experimentSetupMode.name(),nextClusterer.name);
		return ret;
	}
	

}
