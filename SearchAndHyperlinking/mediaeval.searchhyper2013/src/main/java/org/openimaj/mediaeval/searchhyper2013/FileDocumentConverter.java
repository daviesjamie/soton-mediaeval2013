package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;

/**
 * Converts a file into a Lucene document for importing.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public interface FileDocumentConverter {

	public Document convertFile(File file) throws FileDocumentConverterException;
}