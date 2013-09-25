package org.openimaj.mediaeval.vis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.openimaj.image.colour.RGBColour;
import org.openimaj.mediaeval.data.CursorDateFilter;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne.Training;
import org.openimaj.util.pair.LongLongPair;
import org.openimaj.util.stream.Stream;
import org.openimaj.vis.general.BarVisualisation;
import org.openimaj.vis.general.BarVisualisationBasic;
import org.xml.sax.SAXException;

import twitter4j.internal.logging.Logger;

import com.aetrion.flickr.photos.Photo;


/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class FlickrPhotoTimeVisualiser {

	private Logger logger = Logger.getLogger(FlickrPhotoTimeVisualiser.class);

	/**
	 * @param photos
	 * @param end
	 * @param start
	 */
	public FlickrPhotoTimeVisualiser(Training photos, Date start, Date end) {
		System.out.println(photos.numInstances());
//		LongLongPair minmax = findMinMax(photos);
		LongLongPair minmax = LongLongPair.pair(timeNorm(start.getTime()), timeNorm(end.getTime()));
		double[] data = new double[(int) (minmax.second - minmax.first)+1];
		final Float[][] clusterColours = new Float[data.length][];
		Map<Integer,Float[]> clusterColourMap = new HashMap<Integer,Float[]>();
		for (Integer cluster : photos.keySet()) {
//			clusterColourMap.put(cluster, ColourMap.Hot.apply(i++/(float)photos.size()));
			clusterColourMap.put(cluster, RGBColour.randomColour());
		}
		for (int j = 0; j < clusterColours.length; j++) {
			clusterColours[j] = RGBColour.BLACK;
		}

		for (Photo photo : photos) {
			int time = timeNorm(photo);
			if(time < minmax.first || time > minmax.second) continue;
			int index = (time - (int)minmax.first);
			logger.debug(String.format("Photo: %s, Time: %d, Index: %d",photo.getId(),time,index));
			data[index] = 1;
			clusterColours[index] = clusterColourMap.get(photos.getPhotoCluster(photo));
		}
		BarVisualisationBasic vis = new BarVisualisationBasic(2000,600);
		vis.setInvidiualBarColours(clusterColours);
		vis.setData(data);

		vis.showWindow("Flickr Clusters");
		logger.debug("Number of clusters: " + photos.size());

	}

	private int timeNorm(long t) {
		return (int) (t/1000/60/60);
	}

	@SuppressWarnings("unused")
	private LongLongPair findMinMax(Training photos) {
		long min = Long.MAX_VALUE;
		long max = 0;
		for (Photo photo : photos) {
			int time = timeNorm(photo,true);
			logger.debug(String.format("Photo: %s, Time: %d",photo.getId(),time));
			if(time < 0)
				logger.error(String.format("Photo(%s) has problems",photo.getId()));
			min = Math.min(min, time);
			max = Math.max(max, time);
		}
		return LongLongPair.pair(min, max);
	}

	private int timeNorm(Photo photo) {
		return timeNorm(photo,false);
	}
	private int timeNorm(Photo photo,boolean force) {
		long t = photo.getDateTaken().getTime();
		if(force || t < 1000) t = photo.getDatePosted().getTime();
		return timeNorm(t);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws XMLStreamException
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, ParseException, XMLStreamException {
		String bigFile = "/Users/ss/Experiments/sed2013/sed2013_dataset_train.xml";
		String csv = "/Users/ss/Experiments/sed2013/sed2013_dataset_train_gs.csv";
		File xmlFile = new File(bigFile);
		final SimpleDateFormat df = new SimpleDateFormat("yyyy MM");
		final Date after = df.parse("2007 01");
		final Date before = df.parse("2007 02");
		Stream<Photo> photoStream = new XMLCursorStream(xmlFile,"photo")
		.filter(new CursorDateFilter(after, before))
		.map(new CursorWrapperPhoto());;
		InputStream clStream = new FileInputStream(csv);
		Training dataset = new Training(clStream, photoStream);
		SimpleDateFormat f = new SimpleDateFormat("yyyy MM dd");
		Date start = f.parse("2007 01 01");
		Date end = f.parse("2007 01 20");
		FlickrPhotoTimeVisualiser fpwv = new FlickrPhotoTimeVisualiser(dataset,start,end);
	}

}
