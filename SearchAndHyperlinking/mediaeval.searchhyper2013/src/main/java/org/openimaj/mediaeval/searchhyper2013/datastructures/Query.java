package org.openimaj.mediaeval.searchhyper2013.datastructures;

/**
 * Represents a query for the search task.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Query {
	public String queryID;
	public String queryText;
	public String visualCues;
	
	public Query(String queryID, String queryText, String visualCues) {
		this.queryID = queryID;
		this.queryText = queryText;
		this.visualCues = visualCues;
	}
	
	public String toString() {
		return "(" + queryID + ") " + queryText + " | " + visualCues;
	}
}
