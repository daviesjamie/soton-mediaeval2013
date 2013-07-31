package org.openimaj.mediaeval.evaluation.cluster;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.experiment.evaluation.cluster.ClusterEvaluator;
import org.openimaj.experiment.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.MEClusterAnalyser;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.mediaeval.evaluation.cluster.processor.SpatialDoubleDBSCANWrapper;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestClusterEvaluator{
	class TestShape{
		public TestShape(int cluster) {
			this.cluster = cluster;
		}
		int cluster;
	}
	class Circle extends TestShape{

		public Circle(int cluster) {
			super(cluster);
		}

	}
	class Star extends TestShape{

		public Star(int cluster) {
			super(cluster);
		}

	}
	class Cross extends TestShape{

		public Cross(int cluster) {
			super(cluster);
		}

	}
	class TestShapeFV implements FeatureExtractor<DoubleFV, TestShape>{

		@Override
		public DoubleFV extractFeature(TestShape object) {
			double[] ret = new double[1];
			ret[0] = object.cluster;
			return new DoubleFV(ret);
		}

	}
	private MapBackedDataset<Integer, ListDataset<TestShape>,TestShape> correct;

	/**
	 *
	 */
	@Before
	public void prepare(){
		this.correct = new MapBackedDataset<Integer, ListDataset<TestShape>, TestShape>();
		this.correct.put(0, new ListBackedDataset<TestShape>(
			Arrays.asList(
					(TestShape)new Circle(0),
					(TestShape)new Circle(1),
					(TestShape)new Circle(1),
					(TestShape)new Circle(1),
					(TestShape)new Circle(1)
				)
			)
		);
		this.correct.put(1, new ListBackedDataset<TestShape>(
			Arrays.asList(
					(TestShape)new Star(1),
					(TestShape)new Star(2),
					(TestShape)new Star(2),
					(TestShape)new Star(2)
				)
			)
		);
		this.correct.put(2, new ListBackedDataset<TestShape>(
			Arrays.asList(
					(TestShape)new Cross(0),
					(TestShape)new Cross(0),
					(TestShape)new Cross(0),
					(TestShape)new Cross(0),
					(TestShape)new Cross(0),
					(TestShape)new Cross(1),
					(TestShape)new Cross(2),
					(TestShape)new Cross(2)
				)
			)
		);
	}

	/**
	 * Test evaluation against the stanford cluster tests
	 */
	@Test
	public void test(){
		DBSCANConfiguration<DoubleNearestNeighbours, double[]> conf =
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(
					0.1, 2, new DoubleNearestNeighboursExact.Factory()
			);
		DoubleDBSCAN dbsConf = new DoubleDBSCAN(conf);

		ClusterEvaluator<TestShape, MEAnalysis> eval =
			new ClusterEvaluator<TestClusterEvaluator.TestShape, MEAnalysis>(
				new SpatialDoubleDBSCANWrapper<TestShape>(correct,new TestShapeFV(),dbsConf),
				new MEClusterAnalyser(),
				correct
			);
		MEAnalysis res = eval.analyse(eval.evaluate());
		assertTrue(Math.abs(res.purity - 0.71) < 0.01);
		assertTrue(Math.abs(res.nmi - 0.36) < 0.01);
		assertTrue(res.precision == 0.5);
		assertTrue(res.recall - 0.455 < 0.01);
		assertTrue(Math.abs(res.fscore(1) - 0.48) < 0.01);
		assertTrue(Math.abs(res.fscore(5) - 0.456) < 0.01);
		System.out.println(res.getSummaryReport());

	}
}
