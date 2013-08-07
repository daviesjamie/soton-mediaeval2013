package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.toolopts.MatchingModelType;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.feature.local.keypoints.KeypointLocation;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.hard.KDTreeByteEuclideanAssigner;
import org.terrier.compression.BitFileBuffered;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

public class ImageTerrierSearcher implements VisualSearcher {
	static {
		BasicTerrierConfig.configure();
		try {
			final Field dbl = BitFileBuffered.class.getDeclaredField("DEFAULT_BUFFER_LENGTH");
			dbl.setAccessible(true);
			final Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(dbl, dbl.getModifiers() & ~Modifier.FINAL);
			dbl.setInt(null, 1024 * 1024);
		} catch (final SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected Index index;
	private DoGSIFTEngine engine;
	private KDTreeByteEuclideanAssigner assigner;
	private IndexSearcher meta;

	public ImageTerrierSearcher(File filename, File centroidsFile, IndexSearcher meta) throws IOException {
		this.meta = meta;

		final String filenameStr = filename.getAbsolutePath();
		index = Index.createIndex(filenameStr, "index");

		engine = new DoGSIFTEngine();
		engine.getOptions().setDoubleInitialImage(false);
		final ByteCentroidsResult centroids = IOUtils.read(centroidsFile, ByteCentroidsResult.class);
		assigner = new KDTreeByteEuclideanAssigner(centroids);
	}

	@Override
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException {
		final FImage img = Transforms.calculateIntensityNTSC_LUT(query);

		final LocalFeatureList<Keypoint> f = engine.findFeatures(img);
		final List<QuantisedLocalFeature<KeypointLocation>> qf = BagOfVisualWords.computeQuantisedFeatures(assigner, f);

		final QLFDocument<QuantisedLocalFeature<KeypointLocation>> d =
				new QLFDocument<QuantisedLocalFeature<KeypointLocation>>(qf, "query", null);
		final QLFDocumentQuery<QuantisedLocalFeature<KeypointLocation>> qlf =
				new QLFDocumentQuery<QuantisedLocalFeature<KeypointLocation>>(d);

		final Manager manager = new Manager(index);
		final SearchRequest request = manager.newSearchRequest("foo");
		request.setQuery(qlf);
		MatchingModelType.L1IDF.configureRequest(request, qlf);
		ApplicationSetup.setProperty("ignore.low.idf.terms", "false");
		ApplicationSetup.setProperty("matching.retrieved_set_size", "" + numResults);

		manager.runPreProcessing(request);
		manager.runMatching(request);
		manager.runPostProcessing(request);
		manager.runPostFilters(request);

		final ResultSet rs = request.getResultSet();
		final int[] docids = rs.getDocids();
		final double[] scores = rs.getScores();

		final List<ScoreDoc> results = new ArrayList<ScoreDoc>();
		for (int i = 0; i < docids.length; i++) {

			final long val = Long.parseLong(index.getMetaIndex().getItem("docno", docids[i]));
			final ScoreDoc sd = lookup(val);
			if (sd != null) {
				sd.score = (float) scores[i];
				results.add(sd);
			}
		}

		return results.toArray(new ScoreDoc[results.size()]);
	}

	@Override
	public ScoreDoc[] search(long flickrId, int numResults) throws IOException {
		final ScoreDoc sd = lookup(flickrId);

		if (sd == null) {
			return new ScoreDoc[0];
		}

		final URL url = new URL(meta.doc(sd.doc).get("url"));

		return search(ImageUtilities.readMBF(url), numResults);
	}

	private ScoreDoc lookup(long id) throws IOException {
		final Query q = NumericRangeQuery.newLongRange("id", id, id, true, true);
		final ScoreDoc[] docs = meta.search(q, 1).scoreDocs;
		if (docs.length > 0) {
			final ScoreDoc sd = docs[0];

			return sd;
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		final String luceneIndex =
				"/Volumes/SSD/mediaeval13/placing/places.lucene";

		final Directory directory = new SimpleFSDirectory(new File(luceneIndex));
		final IndexSearcher luceneSearcher = new
				IndexSearcher(DirectoryReader.open(directory));

		final ImageTerrierSearcher searcher = new ImageTerrierSearcher(new File(
				"/Volumes/SSD/mediaeval13/placing/sift1x-quant1m-basic.idx"),
				new File("/Volumes/SSD/mediaeval13/codebooks/mirflickr-1000000-sift-fastkmeans-new.idx"), luceneSearcher);

		final URL queryImage = new URL("http://farm9.staticflickr.com/8356/8287649050_1494112f32.jpg");

		final ScoreDoc[] res = searcher.search(ImageUtilities.readMBF(queryImage), 10);

		for (final ScoreDoc sd : res) {
			System.out.println(luceneSearcher.doc(sd.doc).get("url"));
		}
	}
}
