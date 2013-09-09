package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.hadoop.sequencefile.TextBytesSequenceFileUtility;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;

/**
 * This builds a lucene index from quantised SIFT visual terms (pulling data
 * straight from the hdfs). No idea how well it will work...
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class LuceneQSIFTBuilder {
	public static void main(String[] args) throws IOException {
		final Path[] paths = TextBytesSequenceFileUtility.getFilePaths(
				"hdfs://seurat/data/mediaeval/placing/sift1x-quant1m.seq/", "part-m");

		final String indexPath = "/Volumes/SSD/mediaeval13/visual.lucene";

		final WhitespaceAnalyzer a = new WhitespaceAnalyzer(Version.LUCENE_43);
		final IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, a);
		iwc.setRAMBufferSizeMB(512);
		Directory directory;
		directory = new SimpleFSDirectory(new File(indexPath));
		final IndexWriter indexWriter = new IndexWriter(directory, iwc);

		for (final Path path : paths) {
			System.out.println("Processing " + path);

			final TextBytesSequenceFileUtility sequenceFile = new TextBytesSequenceFileUtility(path.toString(), true);

			for (final Entry<Text, BytesWritable> entry : sequenceFile) {
				final List<QuantisedKeypoint> keys = MemoryLocalFeatureList.read(new ByteArrayInputStream(entry
						.getValue().getBytes()), QuantisedKeypoint.class);

				final StringBuilder sb = new StringBuilder();
				for (final QuantisedKeypoint kp : keys) {
					sb.append(kp.id);
					sb.append(" ");
				}

				final Document doc = new Document();
				doc.add(new LongField("id", Long.parseLong(entry.getKey().toString()), Store.YES));
				doc.add(new TextField("qsift", sb.toString(), Store.YES));
				indexWriter.addDocument(doc);
			}

			sequenceFile.close();
		}
		indexWriter.commit();
		indexWriter.close();
	}
}
