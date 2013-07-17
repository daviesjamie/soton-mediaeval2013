package org.openimaj.mediaeval.feature.extractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseDoubleFV;

/**
 * Given a {@link FeatureExtractor} which can extract {@link Map} of Bags of Words
 * from items and a {@link Dataset} of said items from which to extract a vocabulary and
 * IDF statistics, this extractor can construct (possibly sparse) {@link DoubleFV} instances
 * of item instances
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T> the items understood
 */
public class TFIDF<T> implements FeatureExtractor<SparseDoubleFV, T>{
	final static Logger logger = Logger.getLogger(TFIDF.class);

	private FeatureExtractor<Map<String, Integer>, T> extractor;
	private Map<String, Double> df;

	private int ndocs;
	/**
	 * @param dataset the dataset to extract a vocabulary and IDF stats from
	 * @param bowExtractor the way to extract string counts
	 */
	public TFIDF(Dataset<T> dataset, FeatureExtractor<Map<String,Integer>, T> bowExtractor) {
		this();
		this.extractor = bowExtractor;
		logger.info("Preparing IDF stats for dataset with extractor: " + bowExtractor.getClass().toString());
		for (T t : dataset) {
			update(t);
		}
	}

	private TFIDF(){
		this.extractor = null;
		this.df = new HashMap<String,Double>();
	}

	/**
	 * Just set the extractor, IDF is empty so currently ALL calls to {@link #extractFeature(Object)} will
	 * produce length 0 {@link DoubleFV} instances. use {@link #update(Object)} to make something useful happen
	 * incrementally.
	 * @param bowExtractor
	 */
	public TFIDF(FeatureExtractor<Map<String,Integer>, T> bowExtractor){
		this();
		this.extractor = bowExtractor;
	}

	private double idf(String t) {
		return Math.log(ndocs / df.get(t)) + 1;
	}

	/**
	 * @param otherIDF the IDF stats of another TFIDF
	 * @param ext
	 */
	public TFIDF(TFIDF<?> otherIDF, FeatureExtractor<Map<String, Integer>, T> ext) {
		this.df = otherIDF.df;
		this.extractor = ext;
	}

	@Override
	public SparseDoubleFV extractFeature(T object) {
		SparseDoubleFV ret = new SparseDoubleFV(this.df.size());
		Map<String, Integer> feats = this.extractor.extractFeature(object);
		if(feats.size() == 0) return ret; // a TFIDF feature makes no sense for an empty document!
		int i = 0;
		for (String w : this.df.keySet()) {
			int ftd = 0;
			if(feats.containsKey(w))
				ftd = feats.get(w);
			double tfidf = tf(ftd,feats) * idf(w);
			if(tfidf != 0){
				ret.getVector().set(i, tfidf);
			}
			i++;
		}
		ret.getVector().compact();
		return ret;
	}

	private double tf(int ftd, Map<String, Integer> feats) {
		return Math.sqrt(ftd);
	}

	/**
	 * @param t
	 */
	public void update(T t) {
		for (Entry<String, Integer> wc : extractor.extractFeature(t).entrySet()) {


			Double count = this.df.get(wc.getKey());
			if(count == null) count = 0.;
			this.df.put(wc.getKey(),count + 1); // only adds 1, not the number in this document!
		}
		if(this.ndocs % 1000 == 0){
			logger.info("Seen docs: " + (this.ndocs));
		}
		this.ndocs++;
	}

	/**
	 * @return the df list
	 *
	 */
	public Map<String, Double> getDF() {
		return this.df;
	}

	/**
	 * Select a tfidf vocabulary throwing away features we see too often or not often enough
	 * @param tfidfMin
	 * @param tfidfMax
	 * @return a new {@link TFIDF}
	 */
	public TFIDF<T> getSubVocabulary(int tfidfMin, int tfidfMax) {
		TFIDF<T> ret = new TFIDF<T>();
		ret.extractor = this.extractor;
		ret.ndocs = this.ndocs;
		ret.df = new HashMap<String, Double>();
		for (Entry<String, Double> ent : this.df.entrySet()) {
			if(ent.getValue() > tfidfMin && ent.getValue() < tfidfMax){
				ret.df.put(ent.getKey(), ent.getValue());
			}
		}

		return ret;
	}

	/**
	 * @return underlying extractor
	 */
	public FeatureExtractor<Map<String, Integer>, T> getExtractor() {
		return this.extractor;
	}

}
