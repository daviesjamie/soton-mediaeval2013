package org.openimaj.mediaeval.searchhyper2013.OLD;

import gov.sandia.cognition.learning.function.distance.CosineDistanceMetric;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class CosineSimilarityBatchProcessor {

	/**
	 * Calculates cosine distance between every pair of transcript documents 
	 * within a program. This takes a long time, and often doesn't yield 
	 * anything useful; most distances are very close to 1. 
	 * 
	 * @param args[0] URL of Solr instance to retrieve documents from.
	 * @throws SolrServerException 
	 */
	public static void main(String[] args) {
		SolrServer server = new HttpSolrServer(args[0]);
		
		SolrDocumentList programResults;
		try {
			programResults = SearchUtils.queryServer(server, "type:progmeta", "", 6000000);
		} catch (SolrServerException e2) {
			e2.printStackTrace();
			return;
		}
		
		for (SolrDocument program : programResults) {
			String programString = (String) program.getFieldValue("program");
			
			SolrDocumentList transcriptResults = null;
			try {
				transcriptResults = SearchUtils.queryServer(server, "program:" + programString + 
												" AND type:trans", "", 6000000);
			} catch (Exception e1) {
				e1.printStackTrace();
				// Continue if we can't get the results for this program.
				continue;
			}
			
			if (transcriptResults == null) continue;
			
			CosineDistanceMetric distanceMetric = new CosineDistanceMetric();
			
			SolrDocument[] transcriptResultsArray =
				transcriptResults.toArray(new SolrDocument[0]);
			for (int i = 0; i < transcriptResultsArray.length; i++) {
				SolrDocument doc = transcriptResultsArray[i];
				
				String id = (String) doc.getFieldValue("id");
				
				MapVector docVector = null;
				try {
					docVector = SearchUtils.getTermVector(server, id);
				} catch (Exception e) {
					System.err.println(id);
					e.printStackTrace();
					System.err.println();
					continue;
				}

				for (int j = 0; j < transcriptResultsArray.length; j++) {
					SolrDocument otherDoc = transcriptResultsArray[j];
					
					if (!doc.equals(otherDoc)) {
						String otherId = (String) otherDoc.getFieldValue("id");
						
						MapVector otherDocVector = null;
						try {
							otherDocVector = SearchUtils.getTermVector(server, otherId);
						} catch (Exception e) {
							System.err.println(otherId);
							e.printStackTrace();
							System.err.println();
							continue;
						}
						
						double cosine =
							distanceMetric.evaluate(docVector, otherDocVector);
						
						if (cosine < 1.0) {
							System.out.println(id + " " + otherId + " " + cosine);
						}
					}
				}
			}
		}
	}
}
