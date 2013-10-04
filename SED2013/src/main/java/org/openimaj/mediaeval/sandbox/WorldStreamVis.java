package org.openimaj.mediaeval.sandbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.openimaj.image.colour.ColourMap;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.data.util.SimilarityMatrixReader;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;
import org.openimaj.util.function.Operation;
import org.openimaj.util.function.Predicate;
import org.openimaj.vis.general.BarVisualisationBasic;
import org.openimaj.vis.general.LabelledPointVisualisation;
import org.openimaj.vis.general.LabelledPointVisualisation.LabelledDot;
import org.openimaj.vis.world.WorldMap;
import org.w3c.dom.Document;

import com.aetrion.flickr.photos.Photo;

public class WorldStreamVis {
	
	static String root = "/Users/ss/Experiments/sed2013/sed2013_dataset_train.xml";
	private static File csv = new File("/Users/ss/Experiments/sed2013/sed2013_dataset_train_gs.csv");
	final static Map<Integer,Float[]> clusterColour = new HashMap<Integer, Float[]>();
	public static void main(String[] args) throws XMLStreamException, NumberFormatException, IOException {
		XMLCursorStream photoStream = new XMLCursorStream(new FileInputStream(root),"photo");
		final Map<String, Integer> clusters = SED2013ExpOne.photoClusters(csv);
		
		int xmin = -1;
		int xmax = 1;
		int ymin = 51;
		int ymax = 52;
		final Rectangle visible =  new Rectangle(xmin, ymin, xmax-xmin, ymax-ymin);
		final WorldMap<LabelledDot> wp = new WorldMap<LabelledDot>(600, 500, new LabelledPointVisualisation(),xmin,xmax,ymin,ymax);
		wp.getAxesRenderer().setDrawXTicks(false);
		wp.getAxesRenderer().setDrawYTicks(false);
		wp.getAxesRenderer().setDrawYAxis(false);
		wp.getAxesRenderer().setDrawXAxis(false);
		
		BarVisualisationBasic bargraph = new BarVisualisationBasic(1200, 400);
		
		
		double[] timecount = new double[1000];
		photoStream
		.map(new CursorWrapperPhoto())
		.filter(new Predicate<Photo>() {
			
			@Override
			public boolean test(Photo object) {
				return object.getGeoData()!=null && visible.isInside(new Point2dImpl(object.getGeoData().getLongitude(),object.getGeoData().getLatitude()));
			}
		})
		.filter(new Predicate<Photo>() {
			int seen = 0;
			@Override
			public boolean test(Photo object) {
				return seen++ < 1000;
			}
		})
		.forEach(new Operation<Photo>() {
			
			@Override
			public void perform(Photo object) {
				int cluster = clusters.get(object.getId());
				double y = object.getGeoData().getLatitude();
				double x = object.getGeoData().getLongitude();
				wp.addPoint(x, y, new LabelledDot("",0.05, clusterColour(cluster)));
			}

		});
		wp.showWindow("World Map");
	}

	private static Float[] clusterColour(int cluster) {
		if(!clusterColour.containsKey(cluster)){
			clusterColour.put(cluster,RGBColour.randomColour());
		}
		return clusterColour.get(cluster);
	}
}
