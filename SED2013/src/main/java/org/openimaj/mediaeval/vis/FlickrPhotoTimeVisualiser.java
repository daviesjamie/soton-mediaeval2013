package org.openimaj.mediaeval.vis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.mediaeval.data.XMLFlickrPhotoDataset;
import org.xml.sax.SAXException;

import com.aetrion.flickr.photos.Photo;


public class FlickrPhotoTimeVisualiser {

	public FlickrPhotoTimeVisualiser(Dataset<Photo> photos) {

	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//		InputStream dsStream = FlickrPhotoWorldVisualiser.class.getResourceAsStream("/flickr.photo.xml");
		InputStream dsStream = new FileInputStream("/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml");
		XMLFlickrPhotoDataset dataset = new XMLFlickrPhotoDataset(dsStream);
		FlickrPhotoWorldVisualiser fpwv = new FlickrPhotoWorldVisualiser(dataset);

	}

}
