package org.openimaj.mediaeval.evaluation.datasets;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.mediaeval.data.CursorDateFilter;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.data.XMLFlickrPhotoDataset;
import org.openimaj.mediaeval.evaluation.cluster.ClusterEvaluator;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEClusterAnalyser;
import org.openimaj.mediaeval.evaluation.cluster.processor.DoubleDBSCANClusterer;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.mediaeval.feature.extractor.HaversineSimilarity;
import org.openimaj.mediaeval.feature.extractor.PhotoGeo;
import org.openimaj.mediaeval.feature.extractor.PhotoTime;
import org.openimaj.mediaeval.feature.extractor.PhotoTime.Type;
import org.openimaj.mediaeval.feature.extractor.SimilarityAggregator;
import org.openimaj.mediaeval.feature.extractor.TFIDF;
import org.openimaj.mediaeval.feature.extractor.TimeSimilarity;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.util.stream.Stream;

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
	final static Logger logger = Logger.getLogger(SED2013ExpOne.class);
	/**
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public static class Training extends MapBackedDataset<Integer, ListDataset<Photo>, Photo>{
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
				Photo e = xmlFlickrPhotoDataset.get(photoID);
				if(e!=null) clusterSet.add(e);
			}
			removeEmptyClusters();
			this.invertedCluster = prepareInvertedCluster();

		}

		/**
		 * @param clusterCSV
		 * @param photos
		 * @throws IOException
		 */
		public Training(InputStream clusterCSV, Stream<Photo> photos) throws IOException {
			XMLFlickrPhotoDataset xmlFlickrPhotoDataset = new XMLFlickrPhotoDataset(photos);
			CSVParser parser = new CSVParser(clusterCSV, '\t');
			String[] line;
			logger.info("Loading cluster CSV");
			while((line = parser.getLine()) != null){
				if(line[0].equals("cluster")) continue;
				Integer clusterID = Integer.parseInt(line[0]);
				String photoID = line[1];
				ListDataset<Photo> clusterSet = this.get(clusterID);
				if(clusterSet == null){
					this.put(clusterID, clusterSet = new ListBackedDataset<Photo>());
				}
				Photo e = xmlFlickrPhotoDataset.get(photoID);
				if(e!=null) clusterSet.add(e);
			}
			removeEmptyClusters();
			this.invertedCluster = prepareInvertedCluster();

		}

		private void removeEmptyClusters() {
			Iterator<Entry<Integer, ListDataset<Photo>>> iter = this.entrySet().iterator();
			while(iter.hasNext()){
				Entry<Integer, ListDataset<Photo>> next = iter.next();
				if(next.getValue().numInstances() == 0){
					iter.remove();
				}
			}
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
	public MEAnalysis evalPhotoTime(MapBackedDataset<Integer, ListDataset<Photo>, Photo> ds) {
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

		return eval(ds, fve, dbsConf);
	}

	/**
	 * @param ds the dataset to be clustered
	 * @param fve the feature extractor
	 * @param dbsConf the {@link DBSCANConfiguration}
	 * @return a {@link MEAnalysis} of this experiment
	 */
	public MEAnalysis eval(MapBackedDataset<Integer, ListDataset<Photo>, Photo> ds, FeatureExtractor<DoubleFV, Photo> fve, DoubleDBSCAN dbsConf)
	{
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

	/**
	 * A {@link FeatureExtractor} which produces a similarity vector between a {@link Photo}
	 * and all other photos in a {@link Dataset}. The similarity is populated with the features
	 * from Petkos, Papadopoulos and Kompatsiaris 2012 ICMR paper, measuring similarity of
	 * time uploaded, time taken, geographic location and textual similarity of tags, title and
	 * description. If any features are missing entirley, the similarity is set to {@link Double#NaN}
	 * and should be actively ignored (i.e. not treated as 0).
	 * @param ds the dataset used for the {@link TFIDF} as well as the similarity
	 *
	 * @return a similarity vector constructing {@link FeatureExtractor}
	 */
	public static FeatureExtractor<SparseMatrix, Photo> PPK2012Similarity(Dataset<Photo> ds) {
		List<ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>> comps =
			new ArrayList<DatasetSimilarity.ExtractorComparator<Photo,DoubleFV,DoubleFVComparator>>();
		comps.add(
			new ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>(
				new PhotoTime(Type.POSTED),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>(
				new PhotoTime(Type.TAKEN),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>(
				new PhotoGeo(),
				new HaversineSimilarity()
			)
		);
//		comps.add(
//			new ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>(
//				new TFIDF<Photo>(ds,new PhotoTags(true)),
//				DoubleFVComparison.COSINE_SIM
//			)
//		);
//		comps.add(
//			new ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>(
//				new TFIDF<Photo>(ds, new PhotoTitle()),
//				DoubleFVComparison.COSINE_SIM
//			)
//		);
//		comps.add(
//			new ExtractorComparator<Photo, DoubleFV, DoubleFVComparator>(
//				new TFIDF<Photo>(ds,new PhotoDescription()),
//				DoubleFVComparison.COSINE_SIM
//			)
//		);

		FeatureExtractor<SparseMatrix, Photo> dsSim =
			new DatasetSimilarity<Photo>(ds, comps);
		return dsSim;
	}

	/**
	 * Run a sample experiment
	 * @param args
	 * @throws Throwable
	 */
	public static void main(String[] args) throws Throwable {
		// Some choice experiments
		String bigFile = "/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml";
		logger.info(String.format("Loading dataset: %s ", bigFile));
		File xmlFile = new File(bigFile);
		final SimpleDateFormat df = new SimpleDateFormat("yyyy MM");
		final Date after = df.parse("2007 01");
		final Date before = df.parse("2008 01 ");
		Stream<Photo> photoStream = new XMLCursorStream(xmlFile,"photo")
		.filter(new CursorDateFilter(after, before))
		.map(new CursorWrapperPhoto());;
		logger.info(String.format("Stream loaded and filtered, preparing training dataset"));
		InputStream clStream = new FileInputStream("/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train_gs.csv");
		Training dataset = new Training(clStream, photoStream);

		logger.info(String.format("Loaded dataset: %d photos",dataset.numInstances()));
		FeatureExtractor<SparseMatrix, Photo> dsSim = PPK2012Similarity(dataset);
		FeatureExtractor<DoubleFV, Photo> meanSim = new SimilarityAggregator.Mean<Photo>(dsSim);
		DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf =
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(
				1, 2, 2, new DoubleNearestNeighboursExact.Factory()
			);
		MEAnalysis res = new SED2013ExpOne().eval(dataset, meanSim, new DoubleDBSCAN(conf));
		System.out.println(res.getSummaryReport());
	}
}
