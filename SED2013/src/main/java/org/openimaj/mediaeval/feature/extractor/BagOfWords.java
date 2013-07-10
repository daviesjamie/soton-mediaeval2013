package org.openimaj.mediaeval.feature.extractor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimaj.feature.FeatureExtractor;

/**
 * Construct a {@link Map} of string to count instances of something that contains
 * a String[] in some way
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <T>
 *
 */
public abstract class BagOfWords<T> implements FeatureExtractor<Map<String,Integer>, T>{

	@Override
	public Map<String,Integer> extractFeature(T p) {
		List<String> arr = extractStrings(p);
		Map<String, Integer> ret = new HashMap<String, Integer>();
		for (String string : arr) {
			String str = normalise(string);
			Integer count = ret.get(str);
			if(count == null) count = 0;
			ret.put(str, count+1);
		}
		return ret;
	}

	/**
	 * By default this makes the strings lower case
	 * @param string
	 * @return normalise the string
	 */
	public String normalise(String string) {
		return string.toLowerCase();
	}

	/**
	 * @param p
	 * @return turn the object into an array of strings
	 */
	public abstract List<String> extractStrings(T p);

}
