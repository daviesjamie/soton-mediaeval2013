package org.openimaj.mediaeval.feature.extractor;

import org.openimaj.text.nlp.TweetTokeniser;

import com.aetrion.flickr.photos.Photo;

/**
 * Extract a {@link BagOfWords} from a title using a {@link TweetTokeniser}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoDescription extends TweetTokenBOW<Photo> {

	@Override
	public String extractString(Photo p) {
		return p.getDescription();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
