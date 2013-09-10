package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.util.pair.ObjectDoublePair;
import org.openimaj.util.pair.ObjectIntPair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import uk.ac.soton.ecs.jsh2.mediaeval13.utils.FileUtils;

public class ResultList extends AbstractList<ResultItem> {
	public WikipediaItem wikiItem;
	public String monument;
	public int number;
	public double longitude;
	public double latitude;

	public List<ResultItem> results = new ArrayList<ResultItem>();
	public File base;

	public ResultList(File resultsXml) throws ParserConfigurationException, SAXException, IOException, ParseException {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document resultListDoc = dBuilder.parse(resultsXml);

		this.base = resultsXml.getParentFile().getParentFile();

		monument = resultListDoc.getDocumentElement().getAttribute("monument");

		final NodeList photosList = resultListDoc.getElementsByTagName("photo");
		for (int i = 0; i < photosList.getLength(); i++) {
			results.add(new ResultItem((Element) photosList.item(i), this));
		}

		final Document topicsDoc = dBuilder.parse(new File(base,
				(base.toString().contains("/testset/") ? "testset" : "")
						+ base.getName() + "_topics.xml"));
		final NodeList topicList = topicsDoc.getElementsByTagName("topic");
		for (int i = 0; i < topicList.getLength(); i++) {
			String numberStr = null;
			String titleStr = null;
			String latitudeStr = null;
			String longitudeStr = null;
			String wikiStr = null;
			final NodeList children = topicList.item(i).getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				final String name = children.item(j).getNodeName();

				if (name.equals("number")) {
					numberStr = children.item(j).getTextContent();
				} else if (name.equals("title")) {
					titleStr = children.item(j).getTextContent();
				} else if (name.equals("latitude")) {
					latitudeStr = children.item(j).getTextContent();
				} else if (name.equals("longitude")) {
					longitudeStr = children.item(j).getTextContent();
				} else if (name.equals("wiki")) {
					wikiStr = children.item(j).getTextContent();
				}
			}

			if (monument.equals(titleStr)) {
				this.latitude = Double.parseDouble(latitudeStr);
				this.longitude = Double.parseDouble(longitudeStr);
				this.number = Integer.parseInt(numberStr);
				if (wikiStr.length() > 0)
					wikiItem = new WikipediaItem(this, wikiStr);
				break;
			}
		}
	}

	ResultList() {

	}

	private List<ObjectDoublePair<String>> probModel;
	private List<ObjectDoublePair<String>> tfidfModel;
	private List<ObjectDoublePair<String>> socialtfidfModel;

	public List<ObjectDoublePair<String>> getProbabilisticModel() {
		if (probModel == null) {
			try {
				probModel = parseModel(new File(base, "desctxt" + File.separator + base.getName() + "-probabilistic.txt"));
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return probModel;
	}

	public List<ObjectDoublePair<String>> getTFIDFModel() {
		if (tfidfModel == null) {
			try {
				tfidfModel = parseModel(new File(base, "desctxt" + File.separator + base.getName() + "-tfidf.txt"));
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return tfidfModel;
	}

	public List<ObjectDoublePair<String>> getSocialTFIDFModel() {
		if (socialtfidfModel == null) {
			try {
				socialtfidfModel = parseModel(new File(base, "desctxt" + File.separator + base.getName()
						+ "-social-tfidf.txt"));
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return socialtfidfModel;
	}

	private List<ObjectDoublePair<String>> parseModel(File file) throws IOException {
		final String line = FileUtils.readKeyedLine(file, monument, "\t");
		final String[] parts = line.split("\\s");
		final List<ObjectDoublePair<String>> map = new ArrayList<ObjectDoublePair<String>>();

		for (int i = 0; i < parts.length; i += 2) {
			map.add(new ObjectDoublePair<String>(parts[i].toLowerCase(), Double.parseDouble(parts[i + 1])));
		}

		return map;
	}

	@Override
	public ResultItem get(int index) {
		return results.get(index);
	}

	@Override
	public int size() {
		return results.size();
	}

	private static List<String> loadQueries(File f) throws ParserConfigurationException, SAXException, IOException {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(new InputSource(new InputStreamReader(new FileInputStream(f), "UTF-8")));

		final List<String> queries = new ArrayList<String>();
		final NodeList topicList = doc.getElementsByTagName("title");
		for (int i = 0; i < topicList.getLength(); i++) {
			queries.add(topicList.item(i).getTextContent());
		}
		return queries;
	}

	public static Iterable<ResultList> loadResultSet(File base, boolean devset) throws ParserConfigurationException,
			SAXException, IOException
	{
		final List<File> queries = new ArrayList<File>();
		final String dir = devset ? "devset" : "testset";

		for (final String q : loadQueries(new File(base, dir + "/keywordsGPS/" + (devset ? dir + "keywordsGPS/" : "")
				+ dir
				+ "keywordsGPS_topics.xml")))
		{
			queries.add(new File(base, dir + "/keywordsGPS/" + (devset ? dir + "keywordsGPS/xml/" : "xml/") + q + ".xml"));
		}
		for (final String q : loadQueries(new File(base, dir + "/keywords/" + (devset ? dir + "keywords/" : "") + dir
				+ "keywords_topics.xml")))
		{
			queries.add(new File(base, dir + "/keywords/" + (devset ? dir + "keywords/xml/" : "xml/") + q + ".xml"));
		}

		return new Iterable<ResultList>() {
			@Override
			public Iterator<ResultList> iterator() {
				return new Iterator<ResultList>() {
					final Iterator<File> internal = queries.iterator();

					@Override
					public boolean hasNext() {
						return internal.hasNext();
					}

					@Override
					public ResultList next() {
						try {
							return new ResultList(internal.next());
						} catch (final Exception e) {
							throw new RuntimeException(e);
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public ResultList copy() {
		final ResultList rl = new ResultList();
		rl.base = base;
		rl.latitude = latitude;
		rl.longitude = longitude;
		rl.monument = monument;
		rl.number = number;
		rl.wikiItem = wikiItem;
		rl.results = new ArrayList<ResultItem>(this.results);
		return rl;

	}

	List<ObjectIntPair<String>> titleVocab;

	public List<ObjectIntPair<String>> getTitleVocabulary() {
		if (titleVocab == null) {
			titleVocab = new ArrayList<ObjectIntPair<String>>();

			final TObjectIntHashMap<String> tmp = new TObjectIntHashMap<String>();
			for (final ResultItem ri : this) {
				for (final String tok : ri.title.split(ResultItem.TOKENISER)) {
					tmp.adjustOrPutValue(tok, 1, 1);
				}
			}

			for (final String s : tmp.keySet()) {
				titleVocab.add(new ObjectIntPair<String>(s, tmp.get(s)));
			}
		}
		return titleVocab;
	}

	List<ObjectIntPair<String>> tagVocab;

	public List<ObjectIntPair<String>> getTagVocabulary() {
		if (tagVocab == null) {
			tagVocab = new ArrayList<ObjectIntPair<String>>();

			final TObjectIntHashMap<String> tmp = new TObjectIntHashMap<String>();
			for (final ResultItem ri : this) {
				for (final String tok : ri.tags) {
					tmp.adjustOrPutValue(tok, 1, 1);
				}
			}

			for (final String s : tmp.keySet()) {
				tagVocab.add(new ObjectIntPair<String>(s, tmp.get(s)));
			}
		}
		return tagVocab;
	}

	List<ObjectIntPair<String>> descriptionVocab;

	public List<ObjectIntPair<String>> getDescriptionVocabulary() {
		if (descriptionVocab == null) {
			descriptionVocab = new ArrayList<ObjectIntPair<String>>();

			final TObjectIntHashMap<String> tmp = new TObjectIntHashMap<String>();
			for (final ResultItem ri : this) {
				for (final String tok : ri.description.split(ResultItem.TOKENISER)) {
					tmp.adjustOrPutValue(tok, 1, 1);
				}
			}

			for (final String s : tmp.keySet()) {
				descriptionVocab.add(new ObjectIntPair<String>(s, tmp.get(s)));
			}
		}
		return descriptionVocab;
	}
}
