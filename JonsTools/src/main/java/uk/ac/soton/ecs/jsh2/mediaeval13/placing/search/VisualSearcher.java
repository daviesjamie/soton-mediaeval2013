package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import java.io.IOException;

import org.apache.lucene.search.ScoreDoc;
import org.openimaj.image.MBFImage;

/**
 * Interface for all visual search implementations
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public interface VisualSearcher {
	/**
	 * (Optional) Search with an image
	 * 
	 * @param query
	 * @param numResults
	 * @return
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException;

	public ScoreDoc[] search(long flickrId, int numResults) throws IOException;
}
