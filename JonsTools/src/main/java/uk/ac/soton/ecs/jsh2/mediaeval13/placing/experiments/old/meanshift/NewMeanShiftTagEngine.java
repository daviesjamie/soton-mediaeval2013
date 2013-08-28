package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.old.meanshift;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.openimaj.math.statistics.distribution.MultivariateKernelDensityEstimate;
import org.openimaj.math.statistics.distribution.kernel.StandardUnivariateKernels;
import org.openimaj.ml.clustering.meanshift.ExactMeanShift;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class NewMeanShiftTagEngine implements GeoPositioningEngine {
	protected IndexSearcher searcher;
	protected TLongArrayList skipIds;
	protected List<GeoLocation> prior;
	protected int sampleCount;
	protected double bandwidth;

	public NewMeanShiftTagEngine(File file, TLongArrayList skipIds, int sampleCount, File latlngFile, double bandwidth)
			throws IOException
	{
		final Directory directory = new MMapDirectory(file);
		final IndexReader reader = DirectoryReader.open(directory);
		this.searcher = new IndexSearcher(reader);
		this.skipIds = skipIds;
		this.sampleCount = sampleCount;
		this.bandwidth = bandwidth;

		if (latlngFile != null) {
			prior = Utils.readLatLng(latlngFile, skipIds);
			Collections.shuffle(prior);
			prior = prior.subList(0, sampleCount);
		}
	}

	protected List<GeoLocation> search(String query) {
		try {
			final TermQuery q = new TermQuery(new Term(LuceneIndexBuilder.FIELD_TAGS, query));

			final TopScoreDocCollector collector = TopScoreDocCollector.create(1000000, true);
			searcher.search(q, collector);
			final ScoreDoc[] hits = collector.topDocs().scoreDocs;

			List<GeoLocation> locations = new ArrayList<GeoLocation>(hits.length);
			for (int i = 0; i < hits.length; ++i) {
				final int docId = hits[i].doc;
				final Document d = searcher.doc(docId);

				final long flickrId = Long.parseLong(d.get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(flickrId))
					continue;

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float lon = Float.parseFloat(llstr[0]);
				final float lat = Float.parseFloat(llstr[1]);

				locations.add(new GeoLocation(lat, lon));
			}

			if (locations.size() == 0)
				return locations;

			if (locations.size() > sampleCount) {
				Collections.shuffle(locations);
				locations = locations.subList(0, sampleCount);
			} else {
				while (locations.size() < sampleCount) {
					locations.add(locations.get((int) (Math.random() * locations.size())));
				}
			}

			return locations;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	// private void writeQueryResult(QueryImageData query, List<GeoLocation>
	// pts) {
	// try {
	// final File file = new File("/Volumes/SSD/mediaeval13/placing/expt-geos-"
	// + sampleCount + "-"
	// + (prior != null ? "prior" : "") + "/" + query.flickrId + ".txt");
	// file.getParentFile().mkdirs();
	//
	// final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	// for (final GeoLocation l : pts) {
	// bw.write(l.longitude + " " + l.latitude + "\n");
	// }
	// bw.close();
	// } catch (final Exception e) {
	// e.printStackTrace();
	// }
	// }

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final String[] queryTerms = query.tags.split(" ");

		final List<GeoLocation> pts = search(queryTerms[0]);
		for (int i = 1; i < queryTerms.length; i++) {
			pts.addAll(search(queryTerms[i]));
		}
		if (prior != null) {
			pts.addAll(prior);
		}

		final double[][] data = toArray(pts);

		final MultivariateKernelDensityEstimate kde = new MultivariateKernelDensityEstimate(data,
				StandardUnivariateKernels.Gaussian, bandwidth);
		final ExactMeanShift esm = new ExactMeanShift(kde);

		int bestModeIdx = 0;
		double bestLL = kde.estimateLogProbability(esm.getModes()[0]);
		for (int i = 1; i < esm.getModes().length; i++) {
			final double[] mode = esm.getModes()[i];
			final double ll = kde.estimateLogProbability(mode) * esm.counts[i];
			// final double ll = esm.counts[i];

			if (ll > bestLL) {
				bestLL = ll;
				bestModeIdx = i;
			}
		}

		final double[] bestMode = esm.getModes()[bestModeIdx];

		return new GeoLocationEstimate(bestMode[1], bestMode[0], 1);
	}

	private double[][] toArray(List<GeoLocation> pts) {
		final double[][] data = new double[pts.size()][];

		for (int i = 0; i < data.length; i++)
			data[i] = new double[] { pts.get(i).longitude, pts.get(i).latitude };

		return data;
	}
}
