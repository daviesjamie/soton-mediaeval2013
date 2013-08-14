package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Path;
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
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.feature.local.keypoints.KeypointLocation;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.searchhyper2013.ImageTerrierSearcher.SearchResult;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.hard.KDTreeByteEuclideanAssigner;
import org.terrier.compression.BitFileBuffered;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

public class ImageTerrierSearcher {
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

	public ImageTerrierSearcher(File filename, File centroidsFile) throws IOException {
		final String filenameStr = filename.getAbsolutePath();
		index = Index.createIndex(filenameStr, "index");

		engine = new DoGSIFTEngine();
		engine.getOptions().setDoubleInitialImage(false);
		final ByteCentroidsResult centroids = IOUtils.read(centroidsFile, ByteCentroidsResult.class);
		assigner = new KDTreeByteEuclideanAssigner(centroids);
	}
	
	public class SearchResult {
		String fileName;
		double score;
	}

	public SearchResult[] search(MBFImage query, int numResults) throws IOException {
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

		final List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < docids.length; i++) {

			final String fileName = index.getMetaIndex().getItem("docno", docids[i]);
			
			SearchResult result = new SearchResult();
			result.fileName = fileName;
			result.score = scores[i];
			
			results.add(result);
		}

		return results.toArray(new SearchResult[results.size()]);
	}

	public SearchResult[] search(File keypointFile, int numResults) throws IOException {

		final List<QuantisedLocalFeature> qf = 
				MemoryLocalFeatureList.read(keypointFile, QuantisedLocalFeature.class);

		final QLFDocument<QuantisedLocalFeature> d =
				new QLFDocument<QuantisedLocalFeature>(qf, "query", null);
		final QLFDocumentQuery<QuantisedLocalFeature> qlf =
				new QLFDocumentQuery<QuantisedLocalFeature>(d);

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

		final List<SearchResult> results = new ArrayList<SearchResult>();
		for (int i = 0; i < docids.length; i++) {

			final String fileName = index.getMetaIndex().getItem("docno", docids[i]);
			
			SearchResult result = new SearchResult();
			result.fileName = fileName;
			result.score = scores[i];
			
			results.add(result);
		}

		return results.toArray(new SearchResult[results.size()]);
	}
}