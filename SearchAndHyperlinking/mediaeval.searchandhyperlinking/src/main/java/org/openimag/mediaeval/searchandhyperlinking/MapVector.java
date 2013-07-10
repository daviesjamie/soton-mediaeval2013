package org.openimag.mediaeval.searchandhyperlinking;

import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.VectorEntry;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Sets;

/**
 * A Vector backed by a Map to allow creating and comparing vectors where the
 * number and identities of the dimensions are not known.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class MapVector implements Vector {
	Map<Object, Double> map;

	public MapVector() {
		map = new HashMap<Object, Double>();
	}
	
	public Vector clone() {
		return null;
	}
	
	@Override
	public boolean equals(Vector other, double effectiveZero) {
		if (!(other instanceof MapVector)) return false;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();
		
		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			if (Math.abs(mapVal - otherMapVal) > effectiveZero) return false;
		}
		
		return true;
	}

	public Map<Object, Double> getMap() {
		return map;
	}

	@Override
	public Vector plus(Vector other) {
		if (!(other instanceof MapVector)) return null;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();
		
		MapVector newVector = new MapVector();
		Map<Object, Double> newVectorMap = newVector.getMap();
		
		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			newVectorMap.put(key, mapVal + otherMapVal);
		}
		
		return newVector;
	}

	@Override
	public void plusEquals(Vector other) {
		if (!(other instanceof MapVector)) return;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();

		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			map.put(key, mapVal + otherMapVal);
		}
	}

	@Override
	public Vector minus(Vector other) {
		if (!(other instanceof MapVector)) return null;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();
		
		MapVector newVector = new MapVector();
		Map<Object, Double> newVectorMap = newVector.getMap();
		
		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			newVectorMap.put(key, mapVal - otherMapVal);
		}
		
		return newVector;
	}

	@Override
	public void minusEquals(Vector other) {
		if (!(other instanceof MapVector)) return;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();

		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			map.put(key, mapVal - otherMapVal);
		}
	}

	@Override
	public Vector dotTimes(Vector other) {
		if (!(other instanceof MapVector)) return null;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();
		
		MapVector newVector = new MapVector();
		Map<Object, Double> newVectorMap = newVector.getMap();
		
		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			newVectorMap.put(key, mapVal * otherMapVal);
		}
		
		return newVector;
	}

	@Override
	public void dotTimesEquals(Vector other) {
		if (!(other instanceof MapVector)) return;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();

		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			map.put(key, mapVal * otherMapVal);
		}
	}

	@Override
	public Vector scale(double scaleFactor) {
		MapVector newVector = new MapVector();
		Map<Object, Double> newVectorMap = newVector.getMap();
		
		for (Object key : map.keySet()) {
			Double mapVal = map.get(key);
			
			newVectorMap.put(key, mapVal * scaleFactor);
		}
		
		return newVector;
	}

	@Override
	public void scaleEquals(double scaleFactor) {
		for (Object key : map.keySet()) {
			Double mapVal = map.get(key);
			
			map.put(key, mapVal * scaleFactor);
		}
	}

	@Override
	public Vector negative() {
		return scale(-1);
	}

	@Override
	public void negativeEquals() {
		scaleEquals(-1);
	}

	@Override
	public void zero() {
		scaleEquals(0);
	}

	@Override
	public boolean isZero() {
		return equals(new MapVector());
	}

	@Override
	public boolean isZero(double effectiveZero) {
		return equals(new MapVector(), effectiveZero);
	}
	
	public class MapVectorEntry implements VectorEntry {
		Object key;
		
		public MapVectorEntry(Object key) {
			this.key = key;
		}
		
		@Override
		public int getIndex() {
			return 0;
		}

		@Override
		public void setIndex(int index) {
			
		}

		@Override
		public double getValue() {
			return map.get(key);
		}

		@Override
		public void setValue(double value) {
			map.put(key, value);
		}
		
		public MapVectorEntry clone() {
			return null;
		}
		
	}

	@Override
	public Iterator<VectorEntry> iterator() {
		return new Iterator<VectorEntry>() {
			Iterator<Object> keyIter = map.keySet().iterator();

			@Override
			public boolean hasNext() {
				return keyIter.hasNext();
			}

			@Override
			public VectorEntry next() {
				return new MapVectorEntry(keyIter.next());
			}

			@Override
			public void remove() {
				keyIter.remove();
			}
			
		};
	}

	@Override
	public Vector convertToVector() {
		return this;
	}

	@Override
	public void convertFromVector(Vector parameters) {
		map = new HashMap<Object, Double>();
		
		for (VectorEntry entry: parameters) {
			map.put(entry.getIndex(), entry.getValue());
		}
	}

	@Override
	public int getDimensionality() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getElement(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setElement(int index, double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public double dotProduct(Vector other) {
		if (!(other instanceof MapVector)) return (Double) null;
		
		MapVector dotTimes = (MapVector) dotTimes(other);
		
		return dotTimes.sum();
	}

	@Override
	public Matrix outerProduct(Vector other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double angle(Vector other) {
		if (!(other instanceof MapVector)) return (Double) null;
		
		return Math.acos(cosine(other));
	}

	@Override
	public double cosine(Vector other) {
		if (!(other instanceof MapVector)) return (Double) null;
		
		return unitVector().dotProduct(other.unitVector());
	}

	@Override
	public double sum() {
		double sum = 0.0;
		
		for (Object key : map.keySet()) {
			sum += map.get(key);
		}
		
		return sum;
	}

	@Override
	public double norm1() {
		double sum = 0.0;
		
		for (Object key : map.keySet()) {
			sum += Math.abs(map.get(key));
		}
		
		return sum;
	}

	@Override
	public double norm2() {
		double sum = 0.0;
		
		for (Object key : map.keySet()) {
			sum += Math.pow(map.get(key), 2);
		}
		
		return Math.sqrt(sum);
	}

	@Override
	public double norm2Squared() {
		return Math.pow(norm2(), 2);
	}

	@Override
	public double normInfinity() {
		double max = 0.0;
		
		for (Object key : map.keySet()) {
			double val = Math.abs(map.get(key));
			
			if (max < val) val = max;
		}
		
		return max;
	}

	@Override
	public double euclideanDistance(Vector other) {
		if (!(other instanceof MapVector)) return (Double) null;
		
		Map<Object, Double> otherMap = ((MapVector) other).getMap();
		
		double dist = 0.0;
		
		for (Object key : Sets.union(map.keySet(), otherMap.keySet())) {
			Double mapVal = map.get(key);
			Double otherMapVal = otherMap.get(key);

			if (mapVal == null) mapVal = 0d;
			if (otherMapVal == null) otherMapVal = 0d;
			
			dist += Math.pow(mapVal - otherMapVal, 2);
		}
		
		return Math.sqrt(dist);
	}

	@Override
	public double euclideanDistanceSquared(Vector other) {
		if (!(other instanceof MapVector)) return (Double) null;
		
		return Math.pow(euclideanDistance(other), 2);
	}

	@Override
	public Vector times(Matrix matrix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector unitVector() {
		return scale(1 / norm2());
	}

	@Override
	public void unitVectorEquals() {
		scaleEquals(1 / norm2());
	}

	@Override
	public boolean checkSameDimensionality(Vector other) {
		return other instanceof MapVector;
	}

	@Override
	public void assertSameDimensionality(Vector other) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assertDimensionalityEquals(int otherDimensionality) {
		// TODO Auto-generated method stub

	}

	@Override
	public Vector stack(Vector other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector subVector(int minIndex, int maxIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString(NumberFormat format) {
		return map.toString();
	}

	@Override
	public String toString(NumberFormat format, String delimiter) {
		return map.toString();
	}

	@Override
	public boolean isUnitVector() {
		return norm2() == 1.0;
	}

	@Override
	public boolean isUnitVector(double tolerance) {
		return norm2() > 1.0 - tolerance &&
			   norm2() < 1.0 + tolerance;
	}

}
