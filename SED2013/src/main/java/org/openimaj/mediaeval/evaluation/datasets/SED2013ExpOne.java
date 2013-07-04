package org.openimaj.mediaeval.evaluation.datasets;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.mediaeval.data.XMLFlickrPhotoDataset;
import org.openimaj.mediaeval.evaluation.cluster.ClusterEvaluator;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEClusterAnalyser;
import org.openimaj.mediaeval.evaluation.cluster.processor.DoubleDBSCANClusterer;
import org.openimaj.mediaeval.feature.extractor.PhotoTime;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.xml.sax.SAXException;

import twitter4j.internal.logging.Logger;

import com.Ostermiller.util.CSVParser;
import com.aetrion.flickr.photos.Photo;

/**
 * The Flickr Image event detection experiment from MediaEval SED2013.
 * This class allows retrieval of grouped datasets for training.
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013ExpOne {
	Logger logger = Logger.getLogger(SED2013ExpOne.class);
	/**
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public static class Training extends MapBackedDataset<Integer, ListDataset<Photo>, Photo>{
		private XMLFlickrPhotoDataset fullDataset;
		private Logger logger = Logger.getLogger(Training.class);
		private Map<Photo, Integer> invertedCluster;

		/**
		 * @param clusterCSV a stream (file or other) in CSV format containing clusterID,photoID
		 * @param photosXML a stream (file or other) in XML format containing a flickr photolist usable by {@link XMLFlickrPhotoDataset#XMLFlickrPhotoDataset(java.io.InputStream)}
		 * @throws IOException
		 */
		public Training(InputStream clusterCSV, InputStream photosXML) throws IOException{
			this(clusterCSV,new XMLFlickrPhotoDataset(photosXML));
		}

		/**
		 * @param clusterCSV
		 * @param xmlFlickrPhotoDataset
		 * @throws IOException
		 */
		public Training(InputStream clusterCSV, XMLFlickrPhotoDataset xmlFlickrPhotoDataset) throws IOException {
			CSVParser parser = new CSVParser(clusterCSV, '\t');
			String[] line;
			logger.debug("Loading cluster CSV");
			while((line = parser.getLine()) != null){
				if(line[0].equals("cluster")) continue;
				Integer clusterID = Integer.parseInt(line[0]);
				String photoID = line[1];
				ListDataset<Photo> clusterSet = this.get(clusterID);
				if(clusterSet == null){
					this.put(clusterID, clusterSet = new ListBackedDataset<Photo>());
				}
				clusterSet.add(xmlFlickrPhotoDataset.get(photoID));
			}
			this.fullDataset = xmlFlickrPhotoDataset;
			this.invertedCluster = prepareInvertedCluster();
			
		}

		private Map<Photo, Integer> prepareInvertedCluster() {
			Map<Photo, Integer> ret = new HashMap<Photo,Integer>();
			for (Integer cluster : this.keySet()) {
				for (Photo p : this.get(cluster)) {					
					ret.put(p, cluster);
				}
			}
			return ret;
		}

		/**
		 * @param photo
		 * @return the cluster containing this photo, null if it doesn't exist
		 */
		public Integer getPhotoCluster(Photo photo) {
			return this.invertedCluster.get(photo);
		}

		
	}

	/**
	 * @param ds
	 * @return the analysis
	 */
	public MEAnalysis eval(MapBackedDataset<Integer, ListDataset<Photo>, Photo> ds) {
		return eval(ds,new PhotoTime());
	}

	/**
	 * @param ds the dataset to be clustered
	 * @param fve the feature extractor
	 * @return the {@link MEAnalysis}
	 */
	public MEAnalysis eval(
			MapBackedDataset<Integer, ListDataset<Photo>, Photo> ds,
			FeatureExtractor<DoubleFV, Photo> fve) {
		DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf = 
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(
				1, 900000000000l, 2, new DoubleNearestNeighboursExact.Factory()
			);
		DoubleDBSCAN dbsConf = new DoubleDBSCAN(conf);
		
		ClusterEvaluator<Photo, MEAnalysis> eval = 
			new ClusterEvaluator<Photo, MEAnalysis>(
				new DoubleDBSCANClusterer<Photo>(fve,dbsConf), 
				new MEClusterAnalyser(), 
				ds
			);
		int[][] evaluate = eval.evaluate();
		logger.debug("Expected Classes: " + ds.size());
		logger.debug("Detected Classes: " + evaluate.length);
		MEAnalysis res = eval.analyse(evaluate);
		return res;
	}
}
