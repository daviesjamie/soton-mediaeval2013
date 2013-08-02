package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.feature.local.keypoints.KeypointLocation;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.hard.KDTreeByteEuclideanAssigner;

/**
 * Demonstrate simple Lucene search
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class LuceneQSIFTPlayground {
	public static void main(String[] args) throws IOException, ParseException {
		final Directory visualDirectory = new SimpleFSDirectory(new File("/Volumes/SSD/mediaeval13/visual.lucene"));
		final Directory directory = new SimpleFSDirectory(new File("/Volumes/SSD/mediaeval13/placing/places.lucene"));

		final IndexReader metaReader = DirectoryReader.open(directory);
		final IndexSearcher metaSearcher = new IndexSearcher(metaReader);

		final URL queryImage = new URL("http://farm9.staticflickr.com/8356/8287649050_1494112f32.jpg");
		final DoGSIFTEngine e = new DoGSIFTEngine();
		e.getOptions().setDoubleInitialImage(false);
		final ByteCentroidsResult centroids = IOUtils.read(new File(
				"/Volumes/SSD/mediaeval13/codebooks/mirflickr-1000000-sift-fastkmeans-new.idx"),
				ByteCentroidsResult.class);
		final KDTreeByteEuclideanAssigner assigner = new KDTreeByteEuclideanAssigner(centroids);

		final FImage img = ImageUtilities.readF(queryImage);
		final LocalFeatureList<Keypoint> f = e.findFeatures(img);
		final List<QuantisedLocalFeature<KeypointLocation>> qf = BagOfVisualWords.computeQuantisedFeatures(assigner, f);

		String query = "";
		for (final QuantisedLocalFeature<KeypointLocation> k : qf)
			query += k.id + " ";

		BooleanQuery.setMaxClauseCount(10000);
		final Query q = new QueryParser(Version.LUCENE_43, "qsift", new WhitespaceAnalyzer(Version.LUCENE_43))
				.parse(query);

		System.out.println("Starting search");

		// 3. search
		final int hitsPerPage = 1000;
		final IndexReader reader = DirectoryReader.open(visualDirectory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		final TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(q, collector);
		final ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 4. display results
		System.out.println("Found " + collector.getTotalHits() + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			final int docId = hits[i].doc;
			final Document d = searcher.doc(docId);

			final ScoreDoc[] res = metaSearcher.search(NumericRangeQuery.newLongRange("id", Long.parseLong(d.get("id")),
					Long.parseLong(d.get("id")), true, true), 1).scoreDocs;
			if (res.length > 0) {
				final Document dd = metaSearcher.doc(res[0].doc);
				System.out.println(dd.get("url"));
			}
		}

		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close();
	}
}
