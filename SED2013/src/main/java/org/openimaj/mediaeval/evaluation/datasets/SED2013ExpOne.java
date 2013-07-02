package org.openimaj.mediaeval.evaluation.datasets;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.mediaeval.data.XMLFlickrPhotoDataset;
import org.xml.sax.SAXException;

import com.Ostermiller.util.CSVParser;
import com.aetrion.flickr.photos.Photo;

/**
 * The Flickr Image event detection experiment from MediaEval SED2013.
 * This class allows retrieval of grouped datasets for training.
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013ExpOne {

	/**
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public class Training extends MapBackedDataset<Integer, ListDataset<Photo>, Photo>{
		/**
		 * @param clusterCSV a stream (file or other) in CSV format containing clusterID,photoID
		 * @param photosXML a stream (file or other) in XML format containing a flickr photolist usable by {@link XMLFlickrPhotoDataset#XMLFlickrPhotoDataset(java.io.InputStream)}
		 * @throws ParserConfigurationException
		 * @throws SAXException
		 * @throws IOException
		 */
		public Training(InputStream clusterCSV, InputStream photosXML) throws IOException, SAXException, ParserConfigurationException{
			this(clusterCSV,new XMLFlickrPhotoDataset(photosXML));
		}

		/**
		 * @param clusterCSV
		 * @param xmlFlickrPhotoDataset
		 * @throws IOException
		 */
		public Training(InputStream clusterCSV, XMLFlickrPhotoDataset xmlFlickrPhotoDataset) throws IOException {
			CSVParser parser = new CSVParser(clusterCSV);
			String[] line;
			while((line = parser.getLine()) != null){
				Integer clusterID = Integer.parseInt(line[0]);
				String photoID = line[1];
				ListDataset<Photo> clusterSet = this.get(clusterID);
				if(clusterSet == null){
					this.put(clusterID, clusterSet = new ListBackedDataset<Photo>());
				}
				clusterSet.add(xmlFlickrPhotoDataset.get(photoID));
			}
		}
	}
}
