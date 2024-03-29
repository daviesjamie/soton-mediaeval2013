package org.openimaj.mediaeval.evaluation.cluster;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.experiment.evaluation.cluster.ClusterEvaluator;
import org.openimaj.experiment.evaluation.cluster.RangedDBSCANClusterEvaluator;
import org.openimaj.experiment.evaluation.cluster.analyser.DecisionAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FScoreAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FullMEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FullMEClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.analyser.NMIAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.PurityAnalysis;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.mediaeval.evaluation.cluster.processor.SpatialDoubleExtractor;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;

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
		DoubleNNDBSCAN dbsConf = new DoubleNNDBSCAN(0.1, 2, new DoubleNearestNeighboursExact.Factory());

		ClusterEvaluator<double[][], FullMEAnalysis> eval =
			new ClusterEvaluator<double[][], FullMEAnalysis>(
				dbsConf,
				correct,
				new SpatialDoubleExtractor<TestShape>(new TestShapeFV()),
				new FullMEClusterAnalyser()
			);
		FullMEAnalysis res = eval.analyse(eval.evaluate());
		assertTrue(Math.abs(((PurityAnalysis)res.purity).purity - 0.71) < 0.01);
		assertTrue(Math.abs(((NMIAnalysis)res.nmi).nmi - 0.36) < 0.01);
		assertTrue(((DecisionAnalysis)res.decision).precision() == 0.5);
		assertTrue(((DecisionAnalysis)res.decision).recall() - 0.455 < 0.01);
		assertTrue(((FScoreAnalysis)res.fscore).fscore(1) - 0.48 < 0.01);
		assertTrue(((FScoreAnalysis)res.fscore).fscore(5) - 0.456 < 0.01);
		System.out.println(res.getSummaryReport());

	}
}
