package org.openimaj.mediaeval.feature.extractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

/**
 * Given a {@link FeatureExtractor} which can extract {@link Map} of Bags of Words
 * from items and a {@link Dataset} of said items from which to extract a vocabulary and
 * IDF statistics, this extractor can construct (possibly sparse) {@link DoubleFV} instances
 * of item instances
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T> the items understood
 */
public class TFIDF<T> implements FeatureExtractor<DoubleFV, T>{
	Logger logger = Logger.getLogger(DatasetSimilarity.class);

	private FeatureExtractor<Map<String, Integer>, T> extractor;
	private Map<String, Double> idf;
	/**
	 * @param dataset the dataset to extract a vocabulary and IDF stats from
	 * @param bowExtractor the way to extract string counts
	 */
	public TFIDF(Dataset<T> dataset, FeatureExtractor<Map<String,Integer>, T> bowExtractor) {
		this.extractor = bowExtractor;
		this.idf = new HashMap<String,Double>();
		logger.info("Preparing IDF stats for dataset with extractor: " + bowExtractor.getClass().toString());
		int counter = 0;
		for (T t : dataset) {
			if(counter++ % 1000 == 0){
				logger.info("Done: " + counter);
			}
			for (Entry<String, Integer> wc : extractor.extractFeature(t).entrySet()) {
				Double count = this.idf.get(wc.getKey());
				if(count == null) count = 0.;
				this.idf.put(wc.getKey(),count + 1);
			}
		}
		double ndocs = dataset.numInstances();
		for (String t : this.idf.keySet()) {
			this.idf.put(t, Math.log(ndocs / idf.get(t)) + 1);
		}

	}

	/**
	 * @param otherIDF the IDF stats of another TFIDF
	 * @param ext
	 */
	public TFIDF(TFIDF<?> otherIDF, FeatureExtractor<Map<String, Integer>, T> ext) {
		this.idf = otherIDF.idf;
		this.extractor = ext;
	}

	@Override
	public DoubleFV extractFeature(T object) {
		Map<String, Integer> feats = this.extractor.extractFeature(object);
		DoubleFV ret = new DoubleFV(this.idf.size());
		int i = 0;
		for (Entry<String, Double> w : this.idf.entrySet()) {
			String key = w.getKey();
			if(!feats.containsKey(key)) ret.values[i] = 0;
			else ret.values[i] = Math.sqrt(feats.get(key)) / w.getValue();
			i++;
		}
		return ret;
	}

}
