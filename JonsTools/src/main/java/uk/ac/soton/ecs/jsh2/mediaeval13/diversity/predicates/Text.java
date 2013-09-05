package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.utils.SWTTextDetector;
import uk.ac.soton.ecs.jsh2.mediaeval13.utils.WordCandidate;

public class Text extends FaceDetections {
	public Text() {
		this.detectionThresh = 0.2;
	}

	@Override
	protected String getDetectorName() {
		return "text-detections";
	}

	@Override
	protected List<ObjectDoublePair<Rectangle>> detect(ResultItem object) {
		final FImage img = object.getFImage();
		final List<WordCandidate> words = SWTTextDetector.findWords(img);

		final List<ObjectDoublePair<Rectangle>> rects = new ArrayList<ObjectDoublePair<Rectangle>>();
		for (final WordCandidate wc : words) {
			rects.add(ObjectDoublePair.pair(wc.regularBoundingBox, 1));
		}

		return rects;
	}

	@Override
	public boolean test(ResultItem object) {
		final FImage img = object.getFImage();

		final List<ObjectDoublePair<Rectangle>> dets = getDetections(object);
		double area = 0;
		for (final ObjectDoublePair<Rectangle> wc : dets) {
			area += wc.first.width * wc.first.height;
		}
		if (area / (img.height * img.width) > this.detectionThresh) {
			return false;
		}

		return true;
	}
}
