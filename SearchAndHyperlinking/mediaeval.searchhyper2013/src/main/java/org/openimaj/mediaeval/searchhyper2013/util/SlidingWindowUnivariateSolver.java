package org.openimaj.mediaeval.searchhyper2013.util;

import java.util.Arrays;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

public class SlidingWindowUnivariateSolver<FUNC extends UnivariateFunction> {
	BaseUnivariateSolver<FUNC> solver;
	
	public SlidingWindowUnivariateSolver(BaseUnivariateSolver<FUNC> solver) {
		this.solver = solver;
	}
	
	public double[] solve(int maxEval,
						  FUNC f,
						  double start,
						  double end,
						  double width,
						  double step) {
		final int maxSolutions = (int) (((end - (start + width)) / step) + 1);
		
		double[] solutions = new double[maxSolutions];
		
		int numSolutions = 0;
		
		for (double iterStart = start;
			 iterStart + width <= end;
			 iterStart += step) {
			double solution;
			
			try {
				solution = solver.solve(maxEval,
										f,
										iterStart,
										iterStart + width);
			} catch (TooManyEvaluationsException e) {
				// No root.
				continue;
			}
			
			solutions[numSolutions] = solution;
			
			numSolutions++;
		}
		
		return Arrays.copyOfRange(solutions, 0, numSolutions);
	}
}
