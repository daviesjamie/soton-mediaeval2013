package org.openimaj.mediaeval.evaluation.cluster.analyser;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.experiment.evaluation.cluster.analyser.AdjustedRandomIndexAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.AdjustedRandomIndexClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.analyser.DecisionAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FScoreAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FullMEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.FullMEClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.analyser.NMIAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.PurityAnalysis;

import twitter4j.internal.logging.Logger;



/**
 * Test the evaluation metrics using the example here: 
 * http://nlp.stanford.edu/IR-book/html/htmledition/evaluation-of-clustering-1.html
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestMEClusterAnalyser {
	
	private int[][] correct;
	private int[][] estimate;
	private Logger logger = Logger.getLogger(TestMEClusterAnalyser.class);

	/**
	 * 
	 */
	@Before
	public void prepare(){
		this.correct = new int[][]{
			new int[]{0, 1, 2, 3, 4, 5, 6, 7},
			new int[]{8, 9, 10, 11, 12},
			new int[]{13, 14, 15, 16}
		};
		this.estimate = new int[][]{
			new int[]{0, 1, 2, 3, 4, 8},
			new int[]{5, 9, 10, 11, 12, 13},
			new int[]{6, 7, 14, 15, 16}
		};
	}
	
	@Test
	public void testsimple() throws Exception {
		int[][] c = new int[][]{
				new int[]{0,1},
				new int[]{2,3},
				new int[]{4,5}
		};
		int[][] e = new int[][]{
				new int[]{0},
				new int[]{1},
				new int[]{2},
				new int[]{3},
				new int[]{4},
				new int[]{5},
//				new int[]{},
//				new int[]{2},
		};
		FullMEClusterAnalyser ann = new FullMEClusterAnalyser();
		FullMEAnalysis res = ann.analyse(c,e);
		logger .debug(res.getSummaryReport());
	}
	
	@Test
	public void testAdjRandInd() throws Exception {
		int[][] c = new int[][]{
			new int[]{0,1},
			new int[]{2,3,4,5},
			new int[]{6,7,8,9}
		};
		int[][] e = new int[][]{
			new int[]{0,2},
			new int[]{1,3,4},
			new int[]{5,6,7,8,9}
		};
		
		AdjustedRandomIndexClusterAnalyser ann = new AdjustedRandomIndexClusterAnalyser();
		AdjustedRandomIndexAnalysis res = ann.analyse(c,e);
		logger .debug(res.getSummaryReport());
		assertTrue(Math.abs(res.adjRandInd - 0.313)<0.001);
	}
	
	/**
	 * 
	 */
	@Test
	public void test(){
		FullMEClusterAnalyser ann = new FullMEClusterAnalyser();
		FullMEAnalysis res = ann.analyse(correct, estimate);
		logger .debug(res.getSummaryReport());
		assertTrue(Math.abs(((PurityAnalysis)res.purity).purity - 0.71) < 0.01);
		assertTrue(Math.abs(((NMIAnalysis)res.nmi).nmi - 0.36) < 0.01);
		assertTrue(((DecisionAnalysis)res.decision).precision() == 0.5);
		assertTrue(((DecisionAnalysis)res.decision).recall() - 0.455 < 0.01);
		assertTrue(((FScoreAnalysis)res.fscore).fscore(1) - 0.48 < 0.01);
		assertTrue(((FScoreAnalysis)res.fscore).fscore(5) - 0.456 < 0.01);
	}
	
	
}
