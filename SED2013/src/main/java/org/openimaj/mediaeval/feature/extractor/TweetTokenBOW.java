package org.openimaj.mediaeval.feature.extractor;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.text.nlp.TweetTokeniser;

/**
 * Using the {@link TweetTokeniser} extract tokens from the Title
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <T> The type of object to tokenise
 *
 */
public abstract class TweetTokenBOW<T> extends BagOfWords<T>{

	@Override
	public List<String> extractStrings(T p) {
		TweetTokeniser tok;
		try {
			String str = extractString(p);
			tok = new TweetTokeniser(str);
		} catch (Exception e) {
			return new ArrayList<String>();
		}
		return tok.getStringTokens();
	}

	/**
	 * @param p
	 * @return A string from the object
	 */
	public abstract String extractString(T p) ;

}
