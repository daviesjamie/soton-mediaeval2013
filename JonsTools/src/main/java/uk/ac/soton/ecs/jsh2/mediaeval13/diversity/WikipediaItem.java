package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import static uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem.parseFeature;
import static uk.ac.soton.ecs.jsh2.mediaeval13.utils.FileUtils.readKeyedLine;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.openimaj.feature.DoubleFV;
import org.openimaj.io.FileUtils;

public class WikipediaItem {
	public URL url;

	protected ResultList container;
	protected String folder;
	protected String id;

	public WikipediaItem(ResultList container, String url) throws MalformedURLException {
		this.container = container;
		this.url = new URL(url);

		folder = container.base.getName();
		id = computeId();
	}

	private String computeId() {
		for (final String name : new File(container.base, "imgwiki").list()) {
			if (name.startsWith(container.monument + " (")) {
				return name.substring(0, name.indexOf("."));
			}
		}
		return null;
	}

	/**
	 * Get the Global Colour Naming Histogram Feature for the wiki image
	 * 
	 * @return the Global Colour Naming Histogram Feature for the wiki image
	 */
	public DoubleFV getGCNH() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " CN.csv"), id + ""),
					11);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the Spatial Global Colour Naming Histogram Feature for the wiki image
	 * 
	 * @return the Spatial Global Colour Naming Histogram Feature for the wiki
	 *         image
	 */
	public DoubleFV getGCNH3x3() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " CN3x3.csv"), id + ""),
					11 * 3 * 3);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the HOG Feature for the wiki image
	 * 
	 * @return the HOG Feature for the wiki image @
	 */
	public DoubleFV getHOG() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " HOG.csv"), id + ""), 81);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the spatial HOG Feature for the wiki image
	 * 
	 * @return the Spatial HOG Feature for the wiki image @
	 */
	public DoubleFV getHOG3x3() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " HOG3x3.csv"), id + ""),
					81 * 3 * 3);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the Global Colour Moments Feature for the wiki image
	 * 
	 * @return the Global Colour Moments Feature for the wiki image @
	 */
	public DoubleFV getGCM() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " CM.csv"), id + ""),
					9);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the spatial Global Colour Moments Feature for the wiki image
	 * 
	 * @return the Spatial Global Colour Moments Feature for the wiki image @
	 */
	public DoubleFV getGCM3x3() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " CM3x3.csv"), id + ""),
					9 * 3 * 3);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the Locally Binary Patterns Feature for the wiki image
	 * 
	 * @return the Locally Binary Patterns Feature for the wiki image @
	 */
	public DoubleFV getLBP() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " LBP.csv"), id + ""), 16);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the spatial Locally Binary Patterns Feature for the wiki image
	 * 
	 * @return the Spatial Locally Binary Patterns Feature for the wiki image @
	 */
	public DoubleFV getLBP3x3() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " LBP3x3.csv"), id + ""),
					16 * 3 * 3);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the Statistics on Gray Level Run Length Matrix Feature for the wiki
	 * image
	 * 
	 * @return the Statistics on Gray Level Run Length Matrix Feature for the
	 *         wiki image @
	 */
	public DoubleFV getGLRLM() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " GLRLM.csv"), id + ""), 44);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the spatial Statistics on Gray Level Run Length Matrix Feature for
	 * the wiki image
	 * 
	 * @return the Spatial Statistics on Gray Level Run Length Matrix Feature
	 *         for the wiki image @
	 */
	public DoubleFV getGLRLM3x3() {
		try {
			return parseFeature(
					readKeyedLine(new File(container.base, "descvis/imgwiki/" + folder + " GLRLM3x3.csv"), id + ""),
					44 * 3 * 3);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the wikipage in plain text format
	 * 
	 * @return the wikipage text @
	 */
	public String getWikiText() {
		final File wikitext = new File(container.base, "wikitext" + File.separator + container.monument + ".txt");

		try {
			if (wikitext.exists()) {
				return FileUtils.readall(wikitext);
			} else {

				final String path = url.getPath();
				final String title = path.substring(path.lastIndexOf("/") + 1);
				final URL queryurl = new URL("http://en.wikipedia.org/w/index.php?action=raw&title=" + title);

				final String rawcontent = IOUtils.toString(queryurl, "UTF8");

				final WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}",
						"http://www.mywiki.com/wiki/${title}");
				String plainStr = wikiModel.render(new PlainTextConverter(), rawcontent);

				plainStr = plainStr.replaceAll("[{]+[^}]*[}]+", "");

				wikitext.getParentFile().mkdirs();
				org.apache.commons.io.FileUtils.write(wikitext, plainStr);
				return plainStr;
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
