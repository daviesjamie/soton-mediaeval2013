package org.openimaj.mediaeval.vis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.mediaeval.data.XMLFlickrPhotoDataset;
import org.openimaj.vis.general.LabelledPointVisualisation;
import org.openimaj.vis.general.LabelledPointVisualisation.LabelledDot;
import org.openimaj.vis.world.WorldMap;
import org.xml.sax.SAXException;

import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class FlickrPhotoWorldVisualiser {
	private Dataset<Photo> ds;
	private WorldMap<LabelledDot> wp;
	Logger logger = Logger.getLogger(FlickrPhotoWorldVisualiser.class);

	public FlickrPhotoWorldVisualiser(Dataset<Photo> photos) {
		this.ds = photos;
		logger.debug("Preparing Map");
		this.wp = new WorldMap<LabelledDot>(1200, 800, new LabelledPointVisualisation() );
		this.wp.getAxesRenderer().setDrawXAxis(false);
		this.wp.getAxesRenderer().setDrawYAxis(false);
//		wp.addPoint( -67.271667, -55.979722, new LabelledDot( "Cape Horn", 1d, RGBColour.WHITE ) );
//		wp.addPoint( -0.1275, 51.507222, new LabelledDot( "London", 1d, RGBColour.WHITE ) );
//		wp.addPoint( 139.6917, 35.689506, new LabelledDot( "Tokyo", 1d, RGBColour.WHITE ) );
//		wp.addHighlightCountry( "cn" );
//		wp.addHighlightCountry( "us", new Float[]{0f,0.2f,1f,1f} );
		int nogeo = 0;
		for (Photo photo : ds) {
			GeoData gd = photo.getGeoData();
			if(gd!=null)
				wp.addPoint(gd.getLongitude(),gd.getLatitude(), new LabelledDot(photo.getId(), 1d,RGBColour.RED));
			else{
				nogeo+=1;
			}
		}
		logger.debug(String.format("%d of %d have no geodata!",nogeo,ds.numInstances()));
		wp.showWindow( "World" );
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//		InputStream dsStream = FlickrPhotoWorldVisualiser.class.getResourceAsStream("/flickr.photo.xml");
		InputStream dsStream = new FileInputStream("/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml");
		XMLFlickrPhotoDataset dataset = new XMLFlickrPhotoDataset(dsStream);
		FlickrPhotoWorldVisualiser fpwv = new FlickrPhotoWorldVisualiser(dataset);

	}
}
