package org.openimaj.mediaeval.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;

/**
 * Present an XML file conaining Flickr Photos as a dataset
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class XMLFlickrPhotoDataset implements Dataset<Photo> {


	private PhotoList pl;
	Logger logger = Logger.getLogger(XMLFlickrPhotoDataset.class);
	private Map<String, Photo> idIndex;

	/**
	 * @param xmlFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public XMLFlickrPhotoDataset(File xmlFile) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		this(new FileInputStream(xmlFile));
	}

	/**
	 * @param is
	 * @throws IOException
	 */
	public XMLFlickrPhotoDataset(InputStream is) throws IOException {
		logger.debug("Loading XML file from InputStream");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document doc = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
		} catch (Exception e) {
			throw new IOException(e);
		}
		pl = PhotoUtils.createPhotoList(doc.getDocumentElement());
		logger.debug("Preparing ID index");
		this.idIndex = new HashMap<String,Photo>();
		for (Photo p : this) {
			this.idIndex.put(p.getId(), p);
		}
	}

	@Override
	public Iterator<Photo> iterator() {
		return new Iterator<Photo>() {
			Iterator<?> iter = pl.iterator();
			@Override
			public void remove() {
				iter.remove();
			}

			@Override
			public Photo next() {
				return (Photo) iter.next();
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}
		};
	}

	@Override
	public Photo getRandomInstance() {
		return this.iterator().next();
	}

	@Override
	public int numInstances() {
		return pl.size();
	}

	/**
	 * @param photoID
	 * @return the photo with this id
	 */
	public Photo get(String photoID) {
		return this.idIndex.get(photoID);
	}

}
