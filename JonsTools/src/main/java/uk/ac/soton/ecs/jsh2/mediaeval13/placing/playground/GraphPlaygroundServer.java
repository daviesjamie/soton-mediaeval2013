package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.data.RandomData;
import org.openimaj.util.pair.ObjectDoublePair;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;

import com.googlecode.jatl.Html;

public class GraphPlaygroundServer {
	public static GraphPlaygroundServer INSTANCE = new GraphPlaygroundServer();
	private IndexSearcher luceneSearcher;
	private LSHSiftGraphSearcher visualSearcher;

	private GraphPlaygroundServer() {
		try {
			final String lshGraphEdges =
					"/Volumes/SSD/mediaeval13/placing/sift1x-dups/edges.txt";
			final String luceneIndex =
					"/Volumes/SSD/mediaeval13/placing/places.lucene";

			final Directory directory = new SimpleFSDirectory(new File(luceneIndex));
			this.luceneSearcher = new
					IndexSearcher(DirectoryReader.open(directory));
			this.visualSearcher = new LSHSiftGraphSearcher(new File(lshGraphEdges),
					5, luceneSearcher);

		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	List<String> randomImages(int n) {
		try {
			final List<String> rnd = new ArrayList<String>();

			final int[] idxs = RandomData.getUniqueRandomInts(n, 0, luceneSearcher.getIndexReader().maxDoc());

			for (final int i : idxs) {
				final Document d = luceneSearcher.doc(i);
				rnd.add(d.get(LuceneIndexBuilder.FIELD_URL));
			}

			return rnd;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	List<ObjectDoublePair<String>> search(String path, boolean expand) {
		try {
			visualSearcher.setExpand(expand);

			final long flickrId = parseURL(path);
			final ScoreDoc[] docs = visualSearcher.search(flickrId, 1000);

			final List<ObjectDoublePair<String>> results = new ArrayList<ObjectDoublePair<String>>();
			for (final ScoreDoc sd : docs) {
				final String url = luceneSearcher.doc(sd.doc).get(LuceneIndexBuilder.FIELD_URL);
				final double score = sd.score;
				results.add(ObjectDoublePair.pair(url, score));
			}

			Collections.sort(results, ObjectDoublePair.SECOND_ITEM_DESCENDING_COMPARATOR);

			return results;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private long parseURL(String path) {
		final Pattern p = Pattern.compile(".*/([0-9]+)[_].*");
		final Matcher m = p.matcher(path);
		m.matches();
		return Long.parseLong(m.group(1));
	}

	public static class RandomImages extends ServerResource {
		@Get("html")
		public String getRandom() {
			final StringWriter writer = new StringWriter();

			new Html(writer) {
				{
					html();
					body();
					makeImages();
					endAll();
					done();
				}

				void makeImages() {
					for (final String path : INSTANCE.randomImages(10)) {
						a().href("search?img=" + path);
						img().src(path).width("180").end();
						end();
					}
				}
			};

			return writer.toString();
		}
	}

	public static class Search extends ServerResource {
		@Get("html")
		public String search() {
			final String img = getQueryValue("img");
			final String expand = getQueryValue("expand");

			final List<ObjectDoublePair<String>> results;
			if (expand == null)
				results = INSTANCE.search(img, false);
			else
				results = INSTANCE.search(img, true);

			final StringWriter writer = new StringWriter();

			new Html(writer) {
				{
					html();
					body();
					div();
					img().src(img).width("180").end();
					br();
					end();
					makeResults();
					endAll();
					done();
				}

				void makeResults() {
					for (final ObjectDoublePair<String> path : results) {
						div().style("float: left");
						a().href("search?img=" + path.first);
						img().src(path.first).width("180").end();
						end();
						div().text("" + path.second).end();
						end();
					}
				}
			};

			return writer.toString();
		}
	}

	public static void main(String[] args) throws Exception {
		final Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8182);
		component.getDefaultHost().attach("/", RandomImages.class);
		component.getDefaultHost().attach("/search", Search.class);
		component.start();
	}
}
