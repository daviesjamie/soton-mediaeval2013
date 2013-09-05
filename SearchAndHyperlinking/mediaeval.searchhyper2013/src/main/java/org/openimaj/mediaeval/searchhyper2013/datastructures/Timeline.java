package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.openimaj.data.identity.Identifiable;
import org.openimaj.image.Image;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.JustifiedTimedFunction;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SynopsisModule.SynopsisFunction;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.openimaj.mediaeval.searchhyper2013.util.UnivariateFunctionDistribution;

import de.jungblut.math.DoubleVector;
import de.jungblut.math.dense.DenseDoubleVector;

public class Timeline implements Identifiable, UnivariateFunction {
	String id;
	Set<JustifiedTimedFunction> functions;
	float[] shotBoundaries;
	
	public Timeline(String id, float[] shotBoundaries) {
		this.id = id;
		this.shotBoundaries = shotBoundaries;
		
		functions = new HashSet<JustifiedTimedFunction>();
	}
	
	public float[] getShotBoundaries() {
		return shotBoundaries;
	}
	
	@Override
	public double value(double time) {
		double interest = 0;
		
		for (UnivariateFunction f : functions) {
			interest += f.value(time);
		}
		
		return interest;
	}
	
	public boolean addFunction(JustifiedTimedFunction f) {
		return functions.add(f);
	}
	
	public void mergeIn(Timeline other) {
		functions.addAll(other.functions);
	}

	@Override
	public String getID() {
		return id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Timeline other = (Timeline) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public JFreeChart plot() {
		final int noSamples = (int) (10 * getEndTime()) + 1;
		
		double[][] data = new double[2][noSamples];
		
		for (int i = 0; i < noSamples; i++) {
			data[0][i] = ((double) i / noSamples) * getEndTime();
			data[1][i] = value(data[0][i]);
		}
		
		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Plot", data);
		
		return ChartFactory.createXYLineChart("Plot",
											  "Time (seconds)",
											  "Value",
											  dataset,
											  PlotOrientation.VERTICAL,
											  false,
											  false,
											  false);
	}
	
	@Override
	public String toString() {
		/*StringBuilder sb = new StringBuilder();
		
		sb.append("Timeline: " + id + "\n");
		sb.append("Functions: \n");
		
		for (UnivariateFunction function : functions) {
			sb.append("\t" + function.toString() + "\n");
		}
		
		sb.append("End time: " + endTime);
		
		return sb.toString();*/
		
		return id + " | Funcs.: " + functions.size() + " | " + Time.StoMS(getEndTime());
	}
	
	public float getEndTime() {
		return shotBoundaries[shotBoundaries.length - 1];
	}

	public List<DoubleVector> sample() {
		final int NUM_SAMPLES = (int) (getEndTime() / 10);
		
		UnivariateFunctionDistribution distribution = 
				new UnivariateFunctionDistribution(this, 0, getEndTime());
		
		double[] samples = distribution.sample(NUM_SAMPLES);
		
		List<DoubleVector> vectors = new ArrayList<DoubleVector>(NUM_SAMPLES);
		
		for (double sample : samples) {
			vectors.add(new DenseDoubleVector(1, sample));
		}
		
		return vectors;
	}
	
	public int numFunctions() {
		return functions.size();
	}

	public boolean containsInstanceOf(Class<? extends UnivariateFunction> class1) {
		for (UnivariateFunction function : functions) {
			if (class1.isInstance(function)) {
				return true;
			}
		}
		
		return false;
	}

	public Set<JustifiedTimedFunction> getFunctions() {
		return functions;
	}
}
