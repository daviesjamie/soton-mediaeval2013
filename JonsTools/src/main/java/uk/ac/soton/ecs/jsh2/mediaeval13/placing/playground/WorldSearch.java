package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.search.IndexSearcher;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.FlickrAPIToken;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.ExtensibleMeanShiftEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.CachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.GeoDensityEstimateProvider;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.PriorEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.ScoreWeightedVisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;

public class WorldSearch {
	private static final File BASE = new File("/Volumes/SSD/mediaeval13/placing/");
	private static final File DEFAULT_LUCENE_INDEX = new File(BASE, "placesutf8.lucene");
	private static final File DEFAULT_LAT_LNG_FILE = new File(BASE, "training_latlng");
	private static final File DEFAULT_CACHE_LOCATION = new File(BASE, "caches");
	private static final File DEFAULT_LSH_EDGES_FILE = new File(BASE, "sift1x-dups/sift1x-lsh-edges-min1-max20.txt");

	private GeoPositioningEngine engine;

	public WorldSearch() throws IOException {
		final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
		final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
		lsh.setExpand(false);

		final GeoDensityEstimateProvider[] providers = {
				new PriorEstimator(DEFAULT_LAT_LNG_FILE),
				new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
				new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)
		};

		this.engine = new ExtensibleMeanShiftEngine(1000, 0.01, providers);
	}

	public static class Search extends ServerResource {
		static Pattern FLICKR_URL = Pattern.compile("http://www.flickr.com/photos/.*/([0-9]*)[/|$]");

		@Get()
		public String search() throws Exception {
			final String url = getQueryValue("url");
			final Matcher m = FLICKR_URL.matcher(url);

			FImage image;
			final List<String> tags = new ArrayList<String>();

			if (m.matches()) {
				final Flickr flickr = makeFlickr();

				final Photo p = flickr.getPhotosInterface().getPhoto(m.group(1));

				for (final Object o : p.getTags()) {
					tags.add(((Tag) o).getValue());
				}

				image = ImageUtilities.readF(new URL(p.getMediumUrl()));
			} else {
				image = ImageUtilities.readF(new URL(url));
			}

			return "";
		}
	}

	private static Flickr makeFlickr() throws ParserConfigurationException {
		final FlickrAPIToken token = DefaultTokenFactory.get(FlickrAPIToken.class);
		if (token.secret == null)
			return new Flickr(token.apikey, new REST(Flickr.DEFAULT_HOST));
		return new Flickr(token.apikey, token.secret, new REST(Flickr.DEFAULT_HOST));
	}

	public static void main(String[] args) throws Exception {
		final Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8182);
		component.getDefaultHost().attach("/search", Search.class);
		component.start();
	}
}
