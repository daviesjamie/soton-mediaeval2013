package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.openimaj.data.identity.Identifiable;
import org.openimaj.image.Image;

public class Timeline implements Identifiable, UnivariateFunction {
	String id;
	public float endTime;
	Set<UnivariateFunction> functions;
	
	public Timeline(String id, float endTime) {
		this.id = id;
		this.endTime = endTime;
		
		functions = new HashSet<UnivariateFunction>();
	}
	
	@Override
	public double value(double time) {
		double interest = 0;
		
		for (UnivariateFunction f : functions) {
			interest += f.value(time);
		}
		
		return interest;
	}
	
	public boolean addFunction(UnivariateFunction f) {
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
		final int noSamples = (int) (10 * endTime) + 1;
		
		double[][] data = new double[2][noSamples];
		
		for (int i = 0; i < noSamples; i++) {
			data[0][i] = ((double) i / noSamples) * endTime;
			data[1][i] = value(data[0][i]);
		}
		
		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Plot", data);
		
		return ChartFactory.createXYLineChart("Plot",
											  "Time (seconds)",
											  "Value",
											  dataset,
											  PlotOrientation.HORIZONTAL,
											  false,
											  false,
											  false);
	}
}
