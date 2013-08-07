package org.openimaj.mediaeval.searchhyper2013.OLD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.xml.sax.SAXException;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import gov.sandia.cognition.evaluator.Evaluator;
import gov.sandia.cognition.learning.algorithm.gradient.GradientDescendableApproximator;
import gov.sandia.cognition.learning.algorithm.minimization.FunctionMinimizerBFGS;
import gov.sandia.cognition.learning.algorithm.minimization.line.LineMinimizerDerivativeBased;
import gov.sandia.cognition.learning.data.InputOutputPair;
import gov.sandia.cognition.math.DifferentiableEvaluator;
import gov.sandia.cognition.math.matrix.NumericalDifferentiator;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.mtj.DenseVector;
import gov.sandia.cognition.math.matrix.mtj.DenseVectorFactoryMTJ;

public class OptimiseSearcher {

	/**
	 * Optimises a searcher with the BFGS method.
	 * 
	 * @param args[0]						 URL of Solr server.
	 * @param args[1]						 File holding queries.
	 * @param args[2]						 File holding query results.
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		DenseVectorFactoryMTJ vectorFactory = new DenseVectorFactoryMTJ();
		
		Vector initVector = vectorFactory.copyValues(3, 2, 10, 0.5f, 100);
		
		FunctionMinimizerBFGS minimizer = 
			new FunctionMinimizerBFGS(new LineMinimizerDerivativeBased(1),
									  initVector,
									  1e-3,
									  100);
		
		SolrServer solrServer = new HttpSolrServer(args[0]);
		
		File queryFile = new File(args[1]);
		File qRelFile = new File(args[2]);
		Map<Query, Set<Result>> expectedResults = ImportUtils.importExpected(queryFile, qRelFile);
		
		InputOutputPair<Vector, Double> result = 
			minimizer.learn(
				new NumericalDifferentiator.VectorJacobian(
					new SearcherEvaluator(
						new ProgramRestrictedTextSearcher(solrServer),
						new MRRReducer(),
						expectedResults,
						600),
					100));
		
		System.out.println(result);
	}

	private static class SearcherEvaluator implements Evaluator<Vector, Double> {
		private Searcher searcher;
		private Evaluator<Vector, Double> statReducer;
		private Map<Query, Set<Result>> expectedResults;
		private float windowSize;
		
		public SearcherEvaluator(Searcher searcher, Evaluator<Vector, Double> statReducer,
								 Map<Query, Set<Result>> expectedResults, float windowSize) {
			this.searcher = searcher;
			this.statReducer = statReducer;
			this.expectedResults = expectedResults;
			this.windowSize = windowSize;
		}
		
		@Override
		public Double evaluate(Vector input) {
			return statReducer.evaluate(evaluateStats(input));
		}
		
		public Vector evaluateStats(Vector input) {
			searcher.setProperties(input);
			
			final int GRANULARITY = 10;
			
			List<Float> rr = new ArrayList<Float>();
			List<Float> gap = new ArrayList<Float>();
			List<Float> asp = new ArrayList<Float>();
			
			for (Query query : expectedResults.keySet()) {
				String fileName = expectedResults.get(query).iterator().next().getProgram();
				float qRelStart = expectedResults.get(query).iterator().next().getStartTime();
				float qRelEnd = expectedResults.get(query).iterator().next().getEndTime();
				
				
				float totalLen = 0;
				boolean relFlag = false;
				
				List<Result> runResults;
				try {
					runResults = searcher.search(query);
				} catch (RuntimeException e) {
					e.printStackTrace();
					continue;
				}
				
				if (runResults == null) continue;
				
				for (int i = 0; i < runResults.size(); i++) {
					Result result = runResults.get(i);
					
					totalLen += result.getEndTime() - result.getStartTime();
					
					if (("v" + result.getProgram()).equals(fileName)) {
						if (qRelStart - windowSize <= result.getJumpInPoint() &&
							result.getJumpInPoint() <= qRelEnd + windowSize) {
								relFlag = true;
								rr.add(1f / (i + 1));
								
								float penalty = 0;
								if (result.getJumpInPoint() <= qRelStart) {
									penalty = (float) Math.ceil((qRelStart - result.getJumpInPoint()) / GRANULARITY);
								} else if (qRelStart < result.getJumpInPoint()) {
									penalty = (float) Math.ceil((result.getJumpInPoint() - qRelStart) / GRANULARITY);
								}
								
								gap.add((1 - (penalty * GRANULARITY / windowSize)/(i+1)));
								
								float relLen = 0;
								if (qRelStart <= result.getStartTime() && result.getEndTime() <= qRelEnd) {
									relLen = result.getEndTime() - result.getStartTime();
								} else if (result.getStartTime() < qRelStart && qRelEnd < result.getEndTime()) {
									relLen = qRelEnd - qRelStart;
								} else if (result.getStartTime() < qRelStart && qRelStart < result.getEndTime() && result.getEndTime() < qRelEnd) {
									relLen = result.getEndTime() - qRelStart;
								} else if (qRelStart < result.getStartTime() && result.getStartTime() < qRelEnd && qRelEnd < result.getEndTime()) {
									relLen = qRelEnd - result.getStartTime();
								}
								
								if (relLen != 0) {
									asp.add(relLen / totalLen);
								} else {
									asp.add(0f);
								}
						}
					}
				}
				
				if (relFlag == false) {
					rr.add(0f);
					gap.add(0f);
					asp.add(0f);
				}
			}
			
			float mrr = 0;
			float mgap = 0;
			float masp = 0;
			
			for (int i = 0; i < rr.size(); i++) {
				mrr += rr.get(i);
				mgap += gap.get(i);
				masp += asp.get(i);
			}
			
			mrr /= rr.size();
			mgap /= rr.size();
			masp /= rr.size();
			
			DenseVectorFactoryMTJ vectorFactory = new DenseVectorFactoryMTJ();
			Vector resultVector = vectorFactory.copyValues(mrr, mgap, masp);
			
			System.out.println(input + ": " + resultVector);
			
			return resultVector;
		}
	}
	
	private static class MRRReducer implements Evaluator<Vector, Double> {

		@Override
		public Double evaluate(Vector input) {
			return 1 - (((1 * input.getElement(0)) + (2 * input.getElement(1)) + (3 * input.getElement(2))) / 6);
		}
		
	}
}
