package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import org.apache.lucene.index.CorruptIndexException;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;
import org.openimaj.ml.clustering.spectral.HardCodedEigenChooser;
import org.openimaj.ml.clustering.spectral.PreparedSpectralClustering;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.ml.clustering.spectral.SpectralIndexedClusters;
import org.openimaj.util.function.Operation;
import org.openimaj.vis.general.BarVisualisationBasic;
import org.openimaj.vis.general.HeatMap;
import org.openimaj.vis.utils.VisualisationUtils;

import ch.akuhn.matrix.DenseMatrix;
import ch.akuhn.matrix.Matrix;
import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.eigenvalues.FewEigenvalues;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SpectralVis {
	
//	private static String expRoot = "/home/ss/Experiments/mediaeval/SED2013";
	private static String expRoot = "/Users/ss/Experiments/sed2013";
	private static String expHome = expRoot  + "/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/";
	private static HashMap<String,HeatMap> maps = new HashMap<String, HeatMap>();
	private static FewEigenvalues fev;
	private static BarVisualisationBasic barvis;

	public static void main(String[] args) throws IOException {
		barvis = new BarVisualisationBasic(800, 30);
		SimilarityMatrixWrapper wrap = new SimilarityMatrixWrapper(expHome  + "/ALL/aggregationMean.mat", 0, 1000);
		saveForPython(wrap);
		
		final GraphLaplacian gl = new GraphLaplacian.Normalised();
		SparseMatrix L = gl.laplacian(wrap.matrix());
		
		double[][] larr = L.asArray();
		displayHeatMap(larr,"Normal Similarity Heatmap");
		
		displayHeatMap(solrDistance("/Users/ss/Experiments/solr/sed2013_train_v2/data/index",0,1000),"Normal Similarity Heatmap");
		
		fev = FewEigenvalues.of(L).greatest(100);
		fev.run();
		requestedEig = fev.value.length;
		preparedClusters();
		
		VisualisationUtils.displaySlider("Choosen Eigen Vectors",0, fev.value.length,new Operation<JSlider>() {
			@Override
			public void perform(JSlider object) {
				if(!object.getValueIsAdjusting()){
					setNClusters(object.getValue());
					preparedClusters();
					
				}
			}
		});
		
		VisualisationUtils.displaySlider("Choosen Eps",0, 200,new Operation<JSlider>() {
			@Override
			public void perform(JSlider object) {
				if(!object.getValueIsAdjusting()){
					setEps(((double)object.getValue() / object.getMaximum()) * 2);
					preparedClusters();
					
				}
			}
		});
		barvis.showWindow("eigenvectors");
		
	}

	private static double[][] solrDistance(String indexFile,int start, int end) throws CorruptIndexException, IOException {
		MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> ds = SpectralSolrSimilarityExperiment.datasetFromSolr(indexFile, start, end);
		double[][] ret = new double[ds.numInstances()][ds.numInstances()];
		int clusterIndex = 0;
		for (Entry<Integer, ListDataset<IndexedPhoto>> cluster : ds.entrySet()) {
			for (IndexedPhoto itemi : cluster.getValue()) {
				for (IndexedPhoto itemj : cluster.getValue()) {
					ret[(int) itemi.first][(int) itemj.first] = 1;
				}
			}
			clusterIndex++;
		}
		return ret;
	}

	private static double dbscanEPS = 0.5;
	private static int requestedEig = 10;
	
	private static void setNClusters(int value) {
		requestedEig = value;
	}
	
	private static void setEps(double value) {
		dbscanEPS = value;
	}

	
	private static void preparedClusters() {
		final PreparedSpectralClustering prep = new PreparedSpectralClustering(new SpectralClusteringConf<double[]>(new DoubleNNDBSCAN(dbscanEPS, 5), requestedEig));
		SpectralIndexedClusters c = prep.cluster(fev);
		displayHeatMap(distances(c.eigenVectors()), "Spectral Heat Map");
		
		barvis.setData(c.eigenValues());
		
	}
	
	private static void displayHeatMap(double[][] larr,String title) {
		HeatMap hm = maps.get(title);
		if(hm == null){
			maps .put(title, hm = new HeatMap(500, 500));
			hm.setData(larr);
			hm.showWindow(title);
		}
		else{
			hm.setData(larr);
		}
	}
	
	private static double[][] distances(double[][] eigenVectors) {
		return distances(DoubleFVComparison.EUCLIDEAN,eigenVectors);
	}
	private static double[][] distances(Vector[] vector) {
		DoubleFVComparison distance = DoubleFVComparison.EUCLIDEAN;
		
		double[][] dvects = new double[vector.length][];
		int i = 0;
		for (Vector ds : vector) {
			double[] arr = new double[ds.size()];
			ds.storeOn(arr, 0);
			dvects[i++] = arr;
		}
		Jama.Matrix mat = new Jama.Matrix(dvects);
		dvects = mat.transpose().getArray();
		return distances(distance, dvects);
	}

	private static double[][] distances(DoubleFVComparison distance,double[][] dvects) {
		int i;
		
		double[][] ret = new double[dvects.length][dvects.length];
//		Matrix mat = new DenseMatrix(dvects);
//		MatlibMatrixUtils.t
		for (i = 0; i < dvects.length; i++) {
			for (int j = i; j < dvects.length; j++) {
				ret[i][j] = ret[j][i] = distance.compare(dvects[i], dvects[j]);
			}
		}
		return ret;
	}

	private static void saveForPython(SimilarityMatrixWrapper wrap)
			throws IOException {
		MLDouble matarr = MatlibMatrixUtils.asMatlab(wrap.matrix());
		ArrayList<MLArray> data = new ArrayList<MLArray>();
		data.add(matarr);
		new MatFileWriter(new File(expHome + "/1000/aggregationMean.matlab"), data);
	}
}
