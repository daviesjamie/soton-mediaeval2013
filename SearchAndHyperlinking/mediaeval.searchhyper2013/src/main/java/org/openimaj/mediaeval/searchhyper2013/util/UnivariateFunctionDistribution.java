package org.openimaj.mediaeval.searchhyper2013.util;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.Well19937c;

public class UnivariateFunctionDistribution extends AbstractRealDistribution {
	
	UnivariateFunction f;
	double min;
	double max;
	
	UnivariateIntegrator integrator;
	double integralValue;
	
	public UnivariateFunctionDistribution(UnivariateFunction f,
										  double min,
										  double max) {
		super(new Well19937c());
		
		this.f = f;
		this.min = min;
		this.max = max;
		
		integrator = new TrapezoidIntegrator(1e-3, 1e-3, 2, 64);
		integralValue =
				integrator.integrate((int) 1e4, f, min, max);
	}
	
	@Override
	public double density(double x) {
		if (x < min || max < x) {
			return 0;
		}
		
		return f.value(x) / integralValue;
	}

	@Override
	public double cumulativeProbability(double x) {
		if (x <= min) {
			return 0;
		} else if (x >= max) {
			return 1;
		}
		
		return integrator.integrate((int) 1e4, f, min, x)
				/ integralValue;
	}

	@Override
	public double getNumericalMean() {
		return Double.NaN;
	}

	@Override
	public double getNumericalVariance() {
		return Double.NaN;
	}

	@Override
	public double getSupportLowerBound() {
		return min;
	}

	@Override
	public double getSupportUpperBound() {
		return max;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return true;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return true;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}

}
