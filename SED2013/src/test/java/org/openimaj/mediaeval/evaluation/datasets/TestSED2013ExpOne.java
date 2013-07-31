package org.openimaj.mediaeval.evaluation.datasets;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.experiment.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne.Training;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarityAggregator;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.stream.Stream;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestSED2013ExpOne {
	private Training ds;
	private SED2013ExpOne expOne;
	private Training dsStream;

	@Before
	public void before() throws IOException, XMLStreamException{
		expOne = new SED2013ExpOne();
		ds = new Training(
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.cluster.csv"),
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.xml")
		);

		Stream<Photo> photoStream =
			new XMLCursorStream(TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.xml"), "photo")
			.map(new CursorWrapperPhoto()).filter(new Predicate<Photo>() {
				int total = 0;
				@Override
				public boolean test(Photo object) {
					return total ++ < 10;
				}
			});
		dsStream = new Training(
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.cluster.csv"),
			photoStream
		);
	}

	@Test
	public void testEval(){
		MEAnalysis res = expOne.evalPhotoTime(ds);
		System.out.println(res.getSummaryReport());
	}

	@Test
	public void testEvalSim(){
		FeatureExtractor<SparseMatrix, Photo> dsSim = new DatasetSimilarity<Photo>(ds, PPK2012ExtractCompare.similarity(ds));
		FeatureExtractor<DoubleFV, Photo> meanSim = new DatasetSimilarityAggregator.Mean<Photo>(dsSim);
		DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf =
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(
				2, 2, new DoubleNearestNeighboursExact.Factory()
			);
		MEAnalysis res = expOne.eval(ds, meanSim, new DoubleDBSCAN(conf));
		System.out.println(res.getSummaryReport());
	}
	@Test
	public void testEvalSimStream(){
		FeatureExtractor<SparseMatrix, Photo> dsSim = new DatasetSimilarity<Photo>(dsStream, PPK2012ExtractCompare.similarity(ds));
		FeatureExtractor<DoubleFV, Photo> meanSim = new DatasetSimilarityAggregator.Mean<Photo>(dsSim);
		DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf =
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(
				2, 2, new DoubleNearestNeighboursExact.Factory()
			);
		MEAnalysis res = expOne.eval(dsStream, meanSim, new DoubleDBSCAN(conf));
		System.out.println(res.getSummaryReport());
	}
}
