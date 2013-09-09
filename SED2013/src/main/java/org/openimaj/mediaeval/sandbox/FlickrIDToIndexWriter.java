package org.openimaj.mediaeval.sandbox;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.CorruptIndexException;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.mediaeval.evaluation.solr.SED2013IndexUtils;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class FlickrIDToIndexWriter {
	public static void main(String[] args) throws CorruptIndexException, IOException {
		String index = "/home/ss/Experiments/solr/sed2013_train_v3/data/index";
		int total = SED2013IndexUtils.countIndexedItems(index);
		Map<String, IndexedPhoto> map = SED2013IndexUtils.flickrIDIndexedPhoto(index, 0, total);
		String mapout = "/home/ss/Experiments/mediaeval/SED2013/sed2013_train_idtoindex.txt";
		
		PrintWriter pw = new PrintWriter(new File(mapout));
		for (Entry<String, IndexedPhoto> ent : map.entrySet()) {
			pw.printf("%s %d\n",ent.getKey(),ent.getValue().first);
		}
		pw.close();
	}
}
