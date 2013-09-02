package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

/**
 * High-level exception for all FileDocumentConverters.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class FileDocumentConverterException extends Exception {

	private static final long serialVersionUID = 3736015640491456332L;

	public FileDocumentConverterException(Exception e) {
		super(e);
	}

}
