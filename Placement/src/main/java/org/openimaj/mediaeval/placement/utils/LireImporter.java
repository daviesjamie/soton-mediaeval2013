package org.openimaj.mediaeval.placement.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.openimaj.mediaeval.placement.data.LireFeatures;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class LireImporter {

	private static final String[] PREFIXES = new String[] {
			"",
			"",
			"cedd ",
			"",
			"edgehistogram;",
			"fcth ",
			"ophist ",
			"gabor ",
			"jhist ",
			"jophist ",
			"scalablecolor;",
			"RGB ",
			"tamura "
	};

	private String SKIPPED_OUTPUT = "data/featureskipped";

	private String indexPath;
	private List<File> inputFiles;
	private List<String> skippedPhotos;

	public LireImporter(String indexPath, List<File> inputFiles) {
		this.indexPath = indexPath;
		this.inputFiles = inputFiles;
	}

	public void run() throws IOException {
		try {
			IndexWriterConfig config = new IndexWriterConfig(LuceneUtils.LUCENE_VERSION, new WhitespaceAnalyzer(
					LuceneUtils.LUCENE_VERSION));
			config.setRAMBufferSizeMB(1024);
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			IndexWriter indexWriter = new IndexWriter(FSDirectory.open(new File(indexPath)), config);

			skippedPhotos = new ArrayList<String>();
			Iterator<File> iterator = inputFiles.iterator();

			while (iterator.hasNext()) {
				File inputFile = iterator.next();
				System.out.println("Processing " + inputFile.getPath() + ".");
				readFile(indexWriter, inputFile);
				System.out.println("Indexing finished.");
			}

			indexWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		System.out.println("Done.");
		System.out.println(skippedPhotos.size() + " photos were skipped.");

		// Save a list of the photos that failed to import
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(SKIPPED_OUTPUT)));
		for (String id : skippedPhotos)
			w.write(id + "\n");
		w.close();
	}

	private void readFile(IndexWriter indexWriter, File inputFile) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(inputFile), 18000);
		String line = null;
		String feature = null;
		int count = 0;

		outerLoop: while ((line = br.readLine()) != null) {
			Document d = new Document();
			String[] parts = line.split(LireFeatures.CSVREGEX);

			try {
				d.add(new LongField(LuceneIndexBuilder.FIELD_ID, Long.parseLong(parts[0]), Store.YES));

				for (int i = 1; i < parts.length; i++) {
					// Format feature string so that LIRE will accept it
					feature = PREFIXES[i - 1] + parts[i];
					if (i - 1 == 10) {
						feature = feature.replaceFirst(" ", ";");
						feature = feature.replaceFirst(" ", ";");
					}

					LireFeature f = (LireFeature) LireFeatures.values()[i - 1].fclass.newInstance();

					f.setStringRepresentation(feature);

					// If basic features, use string representation
					if (i == 2)
						d.add(new StoredField(LireFeatures.values()[i - 1].name, f.getStringRepresentation()));
					else
						d.add(new StoredField(LireFeatures.values()[i - 1].name, f.getByteArrayRepresentation()));
				}
			} catch (Throwable e) {
				e.printStackTrace();
				// System.exit( 1 );
				skippedPhotos.add(parts[0]);
				continue outerLoop;
			}

			indexWriter.addDocument(d);
			count++;

			if (count % 1000 == 0)
				System.out.print('.');

			if (count % 10000 == 0) {
				indexWriter.commit();
				System.out.println(" " + count);
			}
		}

		br.close();
	}

	public static void main(String[] args) throws IOException {
		ArrayList<File> inputFiles = new ArrayList<File>();
		for (int i = 1; i < 10; i++)
			inputFiles.add(new File("data/imagefeatures_" + i));
		inputFiles.add(new File("data/imagefeatures_missingBlocks"));

		LireImporter li = new LireImporter("data/lire-feature-index", inputFiles);
		li.run();
	}
}
