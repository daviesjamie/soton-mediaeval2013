package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.FlickrAPIToken;
import org.openimaj.util.pair.ObjectDoublePair;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.ExtensibleMeanShiftEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.CachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.GeoDensityEstimateProvider;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.PriorEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.ScoreWeightedVisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.VLADSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;
import com.googlecode.jatl.Html;

public class WorldSearch {
	private static final File BASE = new File("/Volumes/SSD/mediaeval13/placing/");
	private static final File DEFAULT_LUCENE_INDEX = new File(BASE, "placesutf8.lucene");
	private static final File DEFAULT_LAT_LNG_FILE = new File(BASE, "training_latlng");
	private static final File DEFAULT_CACHE_LOCATION = new File(BASE, "caches");
	private static final File DEFAULT_VLAD_INDEX = new File(BASE, "vlad-indexes/rgb-sift1x-vlad64n-pca128-pq16-adcnn.idx");
	private static final File DEFAULT_VLAD_FEATURES_FILE = new File(BASE, "vlad-indexes/rgb-sift1x-vlad64n-pca128.dat");

	static Pattern FLICKR_URL = Pattern.compile("http://www.flickr.com/photos/.*/([0-9]*)[/|$].*");

	private GeoPositioningEngine engine;
	private VLADSearcher vlad;
	private IndexSearcher luceneIndex;

	static WorldSearch ws = new WorldSearch();

	public WorldSearch() {
		System.err.println("Initialising WorldSearch");
		try {
			luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
			vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

			final GeoDensityEstimateProvider[] providers = {
					new PriorEstimator(DEFAULT_LAT_LNG_FILE),
					new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
					new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100, 1.0f)
			};

			final TLongArrayList skipIds = new TLongArrayList();
			for (final GeoDensityEstimateProvider p : providers)
				p.setSkipIds(skipIds);

			this.engine = new ExtensibleMeanShiftEngine(1000, 0.01, providers);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		System.err.println("Initialised");
	}

	public static class Search extends ServerResource {
		@Get()
		public String search() throws Exception {
			System.err.println("Hit search");

			final String url = getQueryValue("url");
			final String callback = getQueryValue("callback");
			final Matcher m = FLICKR_URL.matcher(url);

			MBFImage image;
			final List<String> tags = new ArrayList<String>();

			if (m.matches()) {
				final Flickr flickr = makeFlickr();

				final Photo p = flickr.getPhotosInterface().getPhoto(m.group(1));

				for (final Object o : p.getTags()) {
					tags.add(((Tag) o).getValue());
				}

				image = ImageUtilities.readMBF(new URL(p.getMediumUrl()));
			} else {
				image = ImageUtilities.readMBF(new URL(url));
			}

			System.err.println("Geo Estimation");
			final GeoLocationEstimate result = ws.engine.estimateLocation(image, tags.toArray(new String[tags.size()]));

			return String.format("%s({lat:%f,lng:%f})", callback, result.latitude, result.longitude);
		}
	}

	public static class ImageSearch extends ServerResource {
		private List<ObjectDoublePair<String>> imSearch(MBFImage image) {
			try {
				final ScoreDoc[] docs = ws.vlad.search(image, 1000);

				final List<ObjectDoublePair<String>> results = new ArrayList<ObjectDoublePair<String>>();
				for (final ScoreDoc sd : docs) {
					final String url = ws.luceneIndex.doc(sd.doc).get(LuceneIndexBuilder.FIELD_URL);
					final double score = sd.score;
					results.add(ObjectDoublePair.pair(url, score));
				}

				Collections.sort(results, ObjectDoublePair.SECOND_ITEM_DESCENDING_COMPARATOR);

				return results;
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Get("html")
		public String imageSearch() throws Exception {
			final String url = getQueryValue("url");
			final String imageURL;
			final Matcher m = FLICKR_URL.matcher(url);

			MBFImage image;
			final List<String> tags = new ArrayList<String>();

			if (m.matches()) {
				final Flickr flickr = makeFlickr();

				final Photo p = flickr.getPhotosInterface().getPhoto(m.group(1));

				for (final Object o : p.getTags()) {
					tags.add(((Tag) o).getValue());
				}

				image = ImageUtilities.readMBF(new URL(p.getMediumUrl()));
				imageURL = p.getMediumUrl();
			} else {
				image = ImageUtilities.readMBF(new URL(url));
				imageURL = url;
			}

			final List<ObjectDoublePair<String>> results = imSearch(image);

			final StringWriter writer = new StringWriter();

			new Html(writer) {
				{
					html();
					body();
					div();
					img().src(imageURL).width("180").end();
					br();
					end();
					makeResults();
					endAll();
					done();
				}

				void makeResults() {
					for (final ObjectDoublePair<String> path : results) {
						div().style("float: left");
						a().href("imageSearch?url=" + path.first);
						img().src(square(path.first)).width("150").height("150").end();
						end();
						div().text("" + path.second).end();
						end();
					}
				}
			};

			return writer.toString();
		}

		private static String square(String url) {
			return url.replace(".jpg", "_q.jpg");
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
		component.getClients().add(Protocol.CLAP);
		component.getServers().add(Protocol.HTTP, 8182);

		// handle search
		component.getDefaultHost().attach("/search", Search.class);

		// handle image search
		component.getDefaultHost().attach("/imageSearch", ImageSearch.class);

		// handle static content in the
		// uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground package
		final Application application = new Application(component.getContext().createChildContext()) {
			@Override
			public Restlet createInboundRoot() {
				getConnectorService().getClientProtocols().add(Protocol.CLAP);
				getConnectorService().getServerProtocols().add(Protocol.HTTP);

				final Directory directory = new Directory(getContext(),
						"clap://class/uk/ac/soton/ecs/jsh2/mediaeval13/placing/playground/");
				directory.setListingAllowed(false);
				directory.setDeeplyAccessible(false);

				return directory;
			}
		};
		component.getDefaultHost().attach("/", application);

		component.start();
	}
}
