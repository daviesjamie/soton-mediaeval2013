package org.openimaj.mediaeval.feature.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openimaj.text.nlp.TweetTokeniser;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;


/**
 * The photos tags extracted from {@link Photo#getTags()}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class PhotoTags extends BagOfWords<Photo>{

	private boolean tokenise;
	/**
	 * tokenises each tag further
	 */
	public PhotoTags() {
		this.tokenise = true;
	}

	/**
	 * @param tokenise whether to further tokenise each tag
	 */
	public PhotoTags(boolean tokenise) {
		this.tokenise = tokenise;
	}
	@Override
	public List<String> extractStrings(Photo p) {
		Collection<?> tags = p.getTags();
		List<String> ret = new ArrayList<String>();
		for (Object object : tags) {
			Tag tag = (Tag) object;
			if(tokenise){
				try {
					ret.addAll(new TweetTokeniser(tag.getValue()).getStringTokens());
				} catch (Exception e) {}
			}
			else{
				ret.add(tag.getValue());
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
