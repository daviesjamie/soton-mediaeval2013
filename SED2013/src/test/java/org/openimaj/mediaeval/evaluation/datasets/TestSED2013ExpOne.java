package org.openimaj.mediaeval.evaluation.datasets;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.mediaeval.data.XMLFlickrPhotoDataset;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne.Training;
import org.xml.sax.SAXException;

import com.Ostermiller.util.CSVParser;
import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestSED2013ExpOne {
	private Training ds;
	private SED2013ExpOne expOne;

	@Before
	public void before() throws IOException{
		expOne = new SED2013ExpOne();
		ds = expOne.new Training(
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.cluster.csv"), 
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.xml")
		);
	}
	
	@Test
	public void testEval(){
		MEAnalysis res = expOne.eval(ds);
		System.out.println(res.getSummaryReport());
	}
}
