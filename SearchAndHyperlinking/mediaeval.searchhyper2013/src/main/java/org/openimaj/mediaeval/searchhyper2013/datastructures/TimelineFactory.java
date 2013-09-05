package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.openimaj.io.FileUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;

public class TimelineFactory {
	
	IndexSearcher indexSearcher;
	File shotBoundaryDir;
	
	public TimelineFactory(IndexSearcher indexSearcher, File shotBoundaryDir) {
		this.indexSearcher = indexSearcher;
		this.shotBoundaryDir = shotBoundaryDir;
	}
	
	public Timeline makeTimeline(String programme) throws IOException {
		final int FPS = 25;

		// Get shot boundaries
		String[] lines = 
				FileUtils.readlines(
						new File(shotBoundaryDir.getAbsolutePath() + "/" +
								 programme + "/" +
								 programme + ".webm.scenecut"));
		
		String[] frames = lines[1].split(" ");
		
		float[] boundaries = new float[frames.length];
		
		for (int i = 0; i < frames.length; i++) {
			float time = Float.parseFloat(frames[i]) / FPS;
			
			boundaries[i] = time;
		}
		
		return new Timeline(programme, boundaries);
	}
}
