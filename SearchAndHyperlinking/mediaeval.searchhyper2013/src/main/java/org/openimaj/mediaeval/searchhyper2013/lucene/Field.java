package org.openimaj.mediaeval.searchhyper2013.lucene;

/**
 * Fields for Lucene documents.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public enum Field {
	// Common fields.
	Program,
	Type,
	Text,
	
	// Transcript specific.
	Times,
	
	// Synopsis specific.
	Length,
	Title
}
