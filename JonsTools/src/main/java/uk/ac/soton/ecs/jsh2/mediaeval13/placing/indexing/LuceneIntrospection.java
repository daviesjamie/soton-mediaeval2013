package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

public class LuceneIntrospection {
	public static void main(String[] args) throws IOException {
		// final File indexDirectory = new
		// File("/Volumes/SSD/mediaeval13/placing/bigdatasetutf8-withusers-24hrs.lucene");
		final File indexDirectory = new
				File("/Volumes/SSD/mediaeval13/placing/bigdatasetutf8-3.lucene");
		// final File indexDirectory = new
		// File("/Volumes/SSD/mediaeval13/placing/bigdatasetutf8-withusers.lucene");
		final IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory));
		System.out.println(reader.numDocs());
	}
}
