package org.openimaj.mediaeval.evaluation.datasets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.CachingFeatureExtractor;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.FeatureVector;
import org.openimaj.feature.SparseDoubleFV;
import org.openimaj.feature.SparseDoubleFVComparison;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.evaluation.datasets.SED2013SimilarityMatrix.IdentifiablePhoto;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.mediaeval.feature.extractor.HaversineSimilarity;
import org.openimaj.mediaeval.feature.extractor.PhotoDescription;
import org.openimaj.mediaeval.feature.extractor.PhotoGeo;
import org.openimaj.mediaeval.feature.extractor.PhotoTags;
import org.openimaj.mediaeval.feature.extractor.PhotoTime;
import org.openimaj.mediaeval.feature.extractor.PhotoTime.Type;
import org.openimaj.mediaeval.feature.extractor.PhotoTitle;
import org.openimaj.mediaeval.feature.extractor.TFIDF;
import org.openimaj.mediaeval.feature.extractor.TimeSimilarity;
import org.openimaj.mediaeval.feature.extractor.WrappedFeatureExtractor;
import org.openimaj.util.function.Function;

import com.aetrion.flickr.photos.Photo;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PPK2012ExtractCompare {

	private static <T extends FeatureVector> FeatureExtractor<T, Photo> memcache(FeatureExtractor<T, Photo> fe) {
		FeatureExtractor<T,IdentifiablePhoto> toId = new WrappedFeatureExtractor<T,IdentifiablePhoto,Photo>(
			fe,
			new Function<IdentifiablePhoto, Photo>() {
				@Override
				public Photo apply(IdentifiablePhoto in) {
					return in.p;
				}

			}
		);
		FeatureExtractor<T,IdentifiablePhoto> cachedToId = new CachingFeatureExtractor<T,IdentifiablePhoto>(toId);
		return new WrappedFeatureExtractor<T,Photo,IdentifiablePhoto>(
				cachedToId,
				new Function<Photo,IdentifiablePhoto>() {
					@Override
					public IdentifiablePhoto apply(Photo in) {
						return new IdentifiablePhoto(in);
					}

				}
		);
	}

	private static <T extends FeatureVector> FeatureExtractor<T, Photo> memfilecache(FeatureExtractor<T, Photo> fe, String filecache) {
			FeatureExtractor<T,IdentifiablePhoto> toId = new WrappedFeatureExtractor<T,IdentifiablePhoto,Photo>(
				fe,
				new Function<IdentifiablePhoto, Photo>() {
					@Override
					public Photo apply(IdentifiablePhoto in) {
						return in.p;
					}

				}
			);
			FeatureExtractor<T,IdentifiablePhoto> cachedToId = new CachingFeatureExtractor<T, IdentifiablePhoto>(
				new DiskCachingFeatureExtractor<T,IdentifiablePhoto>(new File(filecache), toId)
			);
			return new WrappedFeatureExtractor<T,Photo,IdentifiablePhoto>(
					cachedToId,
					new Function<Photo,IdentifiablePhoto>() {
						@Override
						public IdentifiablePhoto apply(Photo in) {
							return new IdentifiablePhoto(in);
						}

					}
			);
		}
	/**
	 * A {@link FeatureExtractor} which produces a similarity vector between a {@link Photo}
	 * and all other photos in a {@link Dataset}. The similarity is populated with the features
	 * from Petkos, Papadopoulos and Kompatsiaris 2012 ICMR paper, measuring similarity of
	 * time uploaded, time taken, geographic location and textual similarity of tags, title and
	 * description. If any features are missing entirley, the similarity is set to {@link Double#NaN}
	 * and should be actively ignored (i.e. not treated as 0).
	 * @param ds the dataset used for the {@link TFIDF} as well as the similarity
	 *
	 * @return a similarity vector constructing {@link FeatureExtractor}
	 */
	public static List<ExtractorComparator<Photo, ? extends FeatureVector>> similarity(Dataset<Photo> ds) {
		List<ExtractorComparator<Photo, ? extends FeatureVector>> comps =
			new ArrayList<DatasetSimilarity.ExtractorComparator<Photo,? extends FeatureVector>>();
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memcache(new PhotoTime(Type.POSTED)),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memcache(new PhotoTime(Type.TAKEN)),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memcache(new PhotoGeo()),
				new HaversineSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, SparseDoubleFV>(
				memcache(new TFIDF<Photo>(ds,new PhotoTags(true))),
				SparseDoubleFVComparison.COSINE_SIM
			)
		);
		comps.add(
			new ExtractorComparator<Photo, SparseDoubleFV>(
				memcache(new TFIDF<Photo>(ds, new PhotoTitle())),
				SparseDoubleFVComparison.COSINE_SIM
			)
		);
		comps.add(
			new ExtractorComparator<Photo, SparseDoubleFV>(
				memcache(new TFIDF<Photo>(ds,new PhotoDescription())),
				SparseDoubleFVComparison.COSINE_SIM
			)
		);

		return comps;
	}



	/**
	 * A {@link FeatureExtractor} which produces a similarity vector between a {@link Photo}
	 * and all other photos in a {@link Dataset}. The similarity is populated with the features
	 * from Petkos, Papadopoulos and Kompatsiaris 2012 ICMR paper, measuring similarity of
	 * time uploaded, time taken, geographic location and textual similarity of tags, title and
	 * description. If any features are missing entirley, the similarity is set to {@link Double#NaN}
	 * and should be actively ignored (i.e. not treated as 0).
	 * @param tfidfSource the location of a serialised TFIDF source produced using {@link SED2013TFIDFTrainer}
	 *
	 * @return a similarity vector constructing {@link FeatureExtractor}
	 * @throws IOException
	 */
	public static List<ExtractorComparator<Photo, ?>> similarity(String tfidfSource) throws IOException {
		List<ExtractorComparator<Photo, ?>> comps =
			new ArrayList<DatasetSimilarity.ExtractorComparator<Photo,?>>();
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memcache(new PhotoTime(Type.POSTED)),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memcache(new PhotoTime(Type.TAKEN)),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memcache(new PhotoGeo()),
				new HaversineSimilarity()
			)
		);
		List<TFIDF<Photo>> tfidfList = IOUtils.readFromFile(new File(tfidfSource));
		for (TFIDF<Photo> tfidf : tfidfList) {
			comps.add(new ExtractorComparator<Photo, SparseDoubleFV>(
					memcache(tfidf),
					SparseDoubleFVComparison.COSINE_SIM
				));
		}

		return comps;
	}

	/**
	 * A {@link FeatureExtractor} which produces a similarity vector between a {@link Photo}
	 * and all other photos in a {@link Dataset}. The similarity is populated with the features
	 * from Petkos, Papadopoulos and Kompatsiaris 2012 ICMR paper, measuring similarity of
	 * time uploaded, time taken, geographic location and textual similarity of tags, title and
	 * description. If any features are missing entirley, the similarity is set to {@link Double#NaN}
	 * and should be actively ignored (i.e. not treated as 0).
	 * @param tfidfSource the location of a serialised TFIDF source produced using {@link SED2013TFIDFTrainer}
	 * @param filecache where to cache features
	 *
	 * @return a similarity vector constructing {@link FeatureExtractor}
	 * @throws IOException
	 */
	public static List<ExtractorComparator<Photo, ? extends FeatureVector>> similarity(String tfidfSource, String filecache) throws IOException {
		List<ExtractorComparator<Photo, ? extends FeatureVector>> comps =
			new ArrayList<DatasetSimilarity.ExtractorComparator<Photo,? extends FeatureVector>>();
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memfilecache(new PhotoTime(Type.POSTED),filecache + "/time.posted"),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memfilecache(new PhotoTime(Type.TAKEN),filecache + "/time.taken"),
				new TimeSimilarity()
			)
		);
		comps.add(
			new ExtractorComparator<Photo, DoubleFV>(
				memfilecache(new PhotoGeo(),filecache + "/geo"),
				new HaversineSimilarity()
			)
		);
		List<TFIDF<Photo>> tfidfList = IOUtils.readFromFile(new File(tfidfSource));
		for (TFIDF<Photo> tfidf : tfidfList) {
			comps.add(new ExtractorComparator<Photo, SparseDoubleFV>(
				memfilecache(tfidf,filecache + "/tfidf." + tfidf.getExtractor().getClass().getSimpleName()),
				SparseDoubleFVComparison.COSINE_SIM
			));
		}

		return comps;
	}
}
