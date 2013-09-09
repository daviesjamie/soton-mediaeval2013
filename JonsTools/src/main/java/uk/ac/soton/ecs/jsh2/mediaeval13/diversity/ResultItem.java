package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import static uk.ac.soton.ecs.jsh2.mediaeval13.utils.FileUtils.readKeyedLine;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.openimaj.feature.DoubleFV;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.util.pair.ObjectDoublePair;
import org.openimaj.util.pair.ObjectIntPair;
import org.w3c.dom.Element;

public class ResultItem {
	public static final String TOKENISER = "[^\\p{L}]+";

	public ResultList container;

	public Date date_taken;
	public String description;
	public long id;
	public double latitude;
	public int license;
	public double longitude;
	public int nbComments;
	public int rank;
	public String[] tags;
	public String title;
	public URL url_b;
	public String username;
	public int views;

	public ResultItem(Element item, ResultList container) throws MalformedURLException, ParseException {
		this.container = container;

		date_taken = parseDate(item.getAttribute("date_taken"));
		description = item.getAttribute("description");
		id = Long.parseLong(item.getAttribute("id"));
		latitude = Double.parseDouble(item.getAttribute("latitude"));
		license = Integer.parseInt(item.getAttribute("license"));
		longitude = Double.parseDouble(item.getAttribute("longitude"));
		nbComments = Integer.parseInt(item.getAttribute("nbComments"));
		rank = Integer.parseInt(item.getAttribute("rank"));
		tags = item.getAttribute("tags").split(" ");
		title = item.getAttribute("title");
		url_b = new URL(item.getAttribute("url_b"));
		username = item.getAttribute("username");
		views = Integer.parseInt(item.getAttribute("views"));
	}

	private Date parseDate(String dateStr) throws ParseException {
		// "2012-01-05 17:52:34"
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		return sdf.parse(dateStr);
	}

	public File getImageFile() {
		return new File(container.base, "img" + File.separator + container.monument + File.separator + this.id + ".jpg");
	}

	public MBFImage getMBFImage() {
		try {
			return ImageUtilities.readMBF(getImageFile());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public FImage getFImage() {
		try {
			return ImageUtilities.readF(getImageFile());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DoubleFV cn;

	/**
	 * Get the Global Colour Naming Histogram Feature
	 * 
	 * @return the Global Colour Naming Histogram Feature
	 */
	public DoubleFV getCN() {
		if (cn == null) {
			try {
				cn = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " CN.csv"), id + ""),
						11);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return cn;
	}

	private DoubleFV cn3x3;

	/**
	 * Get the Spatial Global Colour Naming Histogram Feature
	 * 
	 * @return the Spatial Global Colour Naming Histogram Feature
	 */
	public DoubleFV getCN3x3() {
		if (cn3x3 == null) {
			try {
				cn3x3 = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " CN3x3.csv"), id
								+ ""),
						11 * 3 * 3);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return cn3x3;
	}

	private DoubleFV hog;

	/**
	 * Get the HOG Feature
	 * 
	 * @return the HOG Feature
	 */
	public DoubleFV getHOG() {
		if (hog == null) {
			try {
				hog = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " HOG.csv"), id + ""),
						81);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return hog;
	}

	private DoubleFV csd;

	/**
	 * Get the Color Structure Descriptor Feature
	 * 
	 * @return the CSD Feature
	 */
	public DoubleFV getCSD() {
		if (csd == null) {
			try {
				csd = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " CSD.csv"), id
								+ ""), 64);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return csd;
	}

	private DoubleFV cm;

	/**
	 * Get the Global Colour Moments Feature
	 * 
	 * @return the Global Colour Moments Feature
	 */
	public DoubleFV getCM() {
		if (cm == null) {
			try {
				cm = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " CM.csv"), id + ""),
						9);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return cm;
	}

	private DoubleFV cm3x3;

	/**
	 * Get the spatial Global Colour Moments Feature
	 * 
	 * @return the Spatial Global Colour Moments Feature
	 */
	public DoubleFV getCM3x3() {
		if (cm3x3 == null) {
			try {
				cm3x3 = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " CM3x3.csv"), id
								+ ""),
						9 * 3 * 3);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return cm3x3;
	}

	private DoubleFV lbp;

	/**
	 * Get the Locally Binary Patterns Feature
	 * 
	 * @return the Locally Binary Patterns Feature
	 */
	public DoubleFV getLBP() {
		if (lbp == null) {
			try {
				lbp = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " LBP.csv"), id + ""),
						16);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return lbp;
	}

	private DoubleFV lbp3x3;

	/**
	 * Get the spatial Locally Binary Patterns Feature
	 * 
	 * @return the Spatial Locally Binary Patterns Feature
	 */
	public DoubleFV getLBP3x3() {
		if (lbp3x3 == null) {
			try {
				lbp3x3 = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " LBP3x3.csv"), id
								+ ""),
						16 * 3 * 3);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return lbp3x3;
	}

	private DoubleFV glrlm;

	/**
	 * Get the Statistics on Gray Level Run Length Matrix Feature
	 * 
	 * @return the Statistics on Gray Level Run Length Matrix Feature
	 */
	public DoubleFV getGLRLM() {
		if (glrlm == null) {
			try {
				glrlm = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " GLRLM.csv"), id
								+ ""),
						44);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return glrlm;
	}

	private DoubleFV glrlm3x3;

	/**
	 * Get the spatial Statistics on Gray Level Run Length Matrix Feature
	 * 
	 * @return the Spatial Statistics on Gray Level Run Length Matrix Feature
	 */
	public DoubleFV getGLRLM3x3() {
		if (glrlm3x3 == null) {
			try {
				glrlm3x3 = parseFeature(
						readKeyedLine(new File(container.base, "descvis/img/" + container.monument + " GLRLM3x3.csv"), id
								+ ""),
						44 * 3 * 3);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		return glrlm3x3;
	}

	static DoubleFV parseFeature(String line, int expected) {
		final String[] parts = line.split(",");

		if (parts.length != expected)
			throw new IllegalArgumentException();

		final DoubleFV fv = new DoubleFV(expected);

		for (int i = 0; i < expected; i++)
			fv.values[i] = Double.parseDouble(parts[i]);

		return fv;
	}

	private DoubleFV probVec;

	public DoubleFV getProbabilisticVector() {
		if (probVec == null)
			probVec = buildVector(container.getProbabilisticModel());
		return probVec;
	}

	private DoubleFV tfidfVec;

	public DoubleFV getTFIDFVector() {
		if (tfidfVec == null)
			tfidfVec = buildVector(container.getTFIDFModel());
		return tfidfVec;
	}

	private DoubleFV stfidfVec;

	public DoubleFV getSocialTFIDFVector() {
		if (stfidfVec == null)
			stfidfVec = buildVector(container.getSocialTFIDFModel());
		return stfidfVec;
	}

	private DoubleFV buildVector(final List<ObjectDoublePair<String>> model) {
		final DoubleFV f = new DoubleFV(model.size());
		final TObjectIntHashMap<String> tokens = getTokens();

		for (int i = 0; i < model.size(); i++) {
			if (tokens.containsKey(model.get(i).first))
				f.values[i] = tokens.get(model.get(i).first) * model.get(i).second;
		}

		return f;
	}

	public TObjectIntHashMap<String> getTokens() {
		final TObjectIntHashMap<String> tokens = new TObjectIntHashMap<String>();

		for (final String tag : tags) {
			tokens.adjustOrPutValue(tag.toLowerCase(), 1, 1);
		}

		for (final String tok : title.split(TOKENISER)) {
			tokens.adjustOrPutValue(tok.toLowerCase(), 1, 1);
		}

		for (final String tok : description.split(TOKENISER)) {
			tokens.adjustOrPutValue(tok.toLowerCase(), 1, 1);
		}

		return tokens;
	}

	public DoubleFV getNormTitleCounts() {
		final List<ObjectIntPair<String>> vocab = container.getTitleVocabulary();
		final List<String> terms = ObjectIntPair.getFirst(vocab);

		final DoubleFV fv = new DoubleFV(terms.size());
		for (final String tok : title.split(TOKENISER)) {
			fv.values[terms.indexOf(tok)]++;
		}
		fv.normaliseFV();

		return fv;
	}

	public DoubleFV getNormTagCounts() {
		final List<ObjectIntPair<String>> vocab = container.getTagVocabulary();
		final List<String> terms = ObjectIntPair.getFirst(vocab);

		final DoubleFV fv = new DoubleFV(terms.size());
		for (final String tok : tags) {
			fv.values[terms.indexOf(tok)]++;
		}
		fv.normaliseFV();

		return fv;
	}

	public DoubleFV getNormDescriptionCounts() {
		final List<ObjectIntPair<String>> vocab = container.getDescriptionVocabulary();
		final List<String> terms = ObjectIntPair.getFirst(vocab);

		final DoubleFV fv = new DoubleFV(terms.size());
		for (final String tok : description.split(TOKENISER)) {
			fv.values[terms.indexOf(tok)]++;
		}
		fv.normaliseFV();

		return fv;
	}

	@Override
	public String toString() {
		return "ResultItem [date_taken=" + date_taken + ", description=" + description + ", id=" + id + ", latitude="
				+ latitude + ", license=" + license + ", longitude=" + longitude + ", nbComments=" + nbComments
				+ ", rank=" + rank + ", tags=" + Arrays.toString(tags) + ", title=" + title + ", url_b=" + url_b
				+ ", username=" + username + ", views=" + views + "]";
	}
}
