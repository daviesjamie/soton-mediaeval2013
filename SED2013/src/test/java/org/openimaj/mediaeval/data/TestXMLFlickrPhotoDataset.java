package org.openimaj.mediaeval.data;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.io.FileUtils;
import org.xml.sax.SAXException;

/**
 * Present an XML file conaining Flickr Photos as a dataset
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestXMLFlickrPhotoDataset {
	/**
	 * the output folder
	 */
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/**
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@Test
	public void testDataset() throws IOException, SAXException, ParserConfigurationException{
		InputStream is = TestXMLFlickrPhotoDataset.class.getResourceAsStream("/flickr.photo.xml");
		XMLFlickrPhotoDataset ds = new XMLFlickrPhotoDataset(is);
		assertTrue(ds.numInstances() == 58);
	}

	/**
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	@Test
	public void testStreamDataset() throws IOException, XMLStreamException{
		InputStream is = TestXMLFlickrPhotoDataset.class.getResourceAsStream("/flickr.photo.xml");
		File f = folder.newFile("flickr.photo.xml");
		FileUtils.copyStreamToFile(is, f);
		XMLFlickrPhotoStreamDataset ds = new XMLFlickrPhotoStreamDataset(f);
		assertTrue(ds.numInstances() == 58);
	}

}
