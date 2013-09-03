package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.processing.algorithm.EqualisationProcessor;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.iterator.TextLineIterable;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class FaceDetections implements Predicate<ResultItem> {
	protected double detectionThresh = 0;
	protected int numThresh = 0;
	protected boolean force = false;

	public List<ObjectDoublePair<Rectangle>> getDetections(ResultItem object) {
		final File f = new File(object.container.base, getDetectorName() + File.separator
				+ object.container.monument
				+ File.separator + object.id + ".txt");

		if (f.exists() && !force) {
			return read(f);
		} else {
			final List<ObjectDoublePair<Rectangle>> rects = detect(object);

			write(rects, f);
			return rects;
		}
	}

	protected String getDetectorName() {
		return "haar-frontalface_alt2+profileface";
	}

	protected List<ObjectDoublePair<Rectangle>> detect(ResultItem object) {
		final List<ObjectDoublePair<Rectangle>> rects = new ArrayList<ObjectDoublePair<Rectangle>>();
		final FImage fImage = object.getFImage().process(new EqualisationProcessor());

		final HaarCascadeDetector ffDet = HaarCascadeDetector.BuiltInCascade.frontalface_alt2.load();
		ffDet.setMinSize(40);
		final List<DetectedFace> ffDets = ffDet.detectFaces(fImage);
		for (final DetectedFace df : ffDets) {
			rects.add(ObjectDoublePair.pair(df.getBounds(), df.getConfidence()));
		}

		final HaarCascadeDetector pfDet = HaarCascadeDetector.BuiltInCascade.profileface.load();
		pfDet.setMinSize(40);
		final List<DetectedFace> pfDets = pfDet.detectFaces(fImage);
		for (final DetectedFace df : pfDets) {
			rects.add(ObjectDoublePair.pair(df.getBounds(), df.getConfidence()));
		}
		return rects;
	}

	protected List<ObjectDoublePair<Rectangle>> read(final File f) {
		final List<ObjectDoublePair<Rectangle>> rects = new ArrayList<ObjectDoublePair<Rectangle>>();

		for (final String line : new TextLineIterable(f)) {
			final String[] parts = line.split(" ");

			final Rectangle r = new Rectangle(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3]));

			rects.add(ObjectDoublePair.pair(r, Double.parseDouble(parts[4])));
		}

		return rects;
	}

	protected void write(List<ObjectDoublePair<Rectangle>> rects, File f) {
		try {
			f.getParentFile().mkdirs();

			final PrintWriter pw = new PrintWriter(new FileWriter(f));
			for (final ObjectDoublePair<Rectangle> p : rects) {
				pw.format("%d %d %d %d %f\n", (int) p.first.x, (int) p.first.y, (int) p.first.width,
						(int) p.first.height, p.second);
			}
			pw.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean test(ResultItem object) {
		int count = 0;

		for (final ObjectDoublePair<Rectangle> r : getDetections(object)) {
			if (r.second > detectionThresh)
				count++;
		}

		return !(count > numThresh);
	}
}
