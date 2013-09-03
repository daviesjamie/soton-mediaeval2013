package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.processing.algorithm.EqualisationProcessor;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class HaarPedestrian extends FaceDetections {
	@Override
	protected String getDetectorName() {
		return "haar-pedestrian";
	}

	@Override
	protected List<ObjectDoublePair<Rectangle>> detect(ResultItem object) {
		final List<ObjectDoublePair<Rectangle>> rects = new ArrayList<ObjectDoublePair<Rectangle>>();
		final FImage fImage = object.getFImage().process(new EqualisationProcessor());

		final HaarCascadeDetector fbDetector = HaarCascadeDetector.BuiltInCascade.fullbody.load();
		final List<DetectedFace> fbDets = fbDetector.detectFaces(fImage);
		for (final DetectedFace df : fbDets) {
			rects.add(ObjectDoublePair.pair(df.getBounds(), df.getConfidence()));
		}

		final HaarCascadeDetector ubDetector =
				HaarCascadeDetector.BuiltInCascade.upperbody.load();
		final List<DetectedFace> ubDets = ubDetector.detectFaces(fImage);
		for (final DetectedFace df : ubDets) {
			rects.add(ObjectDoublePair.pair(df.getBounds(), df.getConfidence()));
		}

		final HaarCascadeDetector mcsubDetector = HaarCascadeDetector.BuiltInCascade.mcs_upperbody.load();
		final List<DetectedFace> mcsubDets = mcsubDetector.detectFaces(fImage);
		for (final DetectedFace df : mcsubDets) {
			rects.add(ObjectDoublePair.pair(df.getBounds(), df.getConfidence()));
		}

		return rects;
	}
}
