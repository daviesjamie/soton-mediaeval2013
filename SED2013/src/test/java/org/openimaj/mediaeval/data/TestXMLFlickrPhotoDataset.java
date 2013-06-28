package org.openimaj.mediaeval.data;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Present an XML file conaining Flickr Photos as a dataset
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestXMLFlickrPhotoDataset {

	@Test
	public void testDataset() throws IOException, SAXException, ParserConfigurationException{
		InputStream is = TestXMLFlickrPhotoDataset.class.getResourceAsStream("/flickr.photo.xml");
		XMLFlickrPhotoDataset ds = new XMLFlickrPhotoDataset(is);
	}

}
