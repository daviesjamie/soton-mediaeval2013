package org.openimag.mediaeval.searchandhyperlinking;

import gov.sandia.cognition.learning.algorithm.clustering.AgglomerativeClusterer;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.Cluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultClusterCreator;
import gov.sandia.cognition.learning.algorithm.clustering.divergence.ClusterToClusterDivergenceFunction;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;
import gov.sandia.cognition.learning.function.distance.CosineDistanceMetric;
import gov.sandia.cognition.util.CloneableSerializable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.NearestNeighboursFactory;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.util.filter.FilterUtils;
import org.openimaj.util.function.Predicate;
import org.xml.sax.SAXException;

public class ProgramRestrictedTextSearcher implements Searcher {
	private SolrServer server;
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		SolrServer serv = new HttpSolrServer("http://seurat:8983/solr");
		ProgramRestrictedTextSearcher searcher = new ProgramRestrictedTextSearcher(serv);
		
		List<Query> queries = Query.readQueriesFromFile(new File("/home/jpreston/Work/data/mediaeval/mediaeval-searchhyper/dev/dev-queries.xml"));
		
		for (Query q : queries) {
			
			List<Result> res = searcher.search(q);

			for (int i = 0; i < res.size(); i++) {
				Result r = res.get(i);
				
				System.out.println(q.getQueryID() + " " +
								  "Q0" + " " +
								  "v" + r.getProgram() + " " +
								  DataUtils.StoMS(r.getStartTime()) + " " +
								  DataUtils.StoMS(r.getEndTime()) + " " +
								  DataUtils.StoMS(r.getJumpInPoint()) + " " +
								  (i + 1) + " " +
								  r.getConfidenceScore() + " " +
								  "run_1");
			}
		}
	}
	
	public ProgramRestrictedTextSearcher(SolrServer server) {
		this.server = server;
	}

	@Override
	public List<Result> search(Query q) {
		DataUtils.cleanQuery(q);
		
		// Maximum number of programs to query in synopsis for.
		final int MAX_SYNOPSIS_RESULTS = 3;
		
		// Store weight and length for each program.
		Map<String, Float> progWeights = new HashMap<String, Float>();
		Map<String, Float> progLengths = new HashMap<String, Float>();
		
		// Get synopsis results.
		String query = "{!df=synopsis} type:progmeta AND (" + q.getQueryText() + ")";
		
		SolrDocumentList results;
		try {
			results = SearchUtils.queryServer(server, query, MAX_SYNOPSIS_RESULTS);
		} catch (SolrServerException e) {
			e.printStackTrace();
			return null;
		}
		
		Set<String> progNames = new HashSet<String>();
		
		float maxScore = results.getMaxScore();
		
		for (int i = 0; i < results.size(); i++) {
			String program = (String) results.get(i).getFieldValue("program");
			float progScore = (Float) results.get(i).getFieldValue("score");
			progWeights.put(program, progScore / maxScore);
			progLengths.put(program, (Float) results.get(i).getFieldValue("length"));
			progNames.add((String) results.get(i).getFieldValue("title"));
		}
		
		// Find program IDs matching titles from synopses, add to list.
		for (String title : progNames) {
			query = "{!df=title} type:progmeta AND \"" + title + "\"";

			try {
				results = SearchUtils.queryServer(server, query, 100);
			} catch (SolrServerException e) {
				e.printStackTrace();
				return null;
			}
			
			// Scale maxScore so that results are skewed in favour of synopsis
			// hits.
			maxScore = (float) (results.getMaxScore() * 1.5);
			
			for (SolrDocument doc : results) {
				String program = (String) doc.getFieldValue("program");
				
				progLengths.put(program, (Float) doc.getFieldValue("length"));
				
				if (!progWeights.containsKey(program)) {
					float progScore = (Float) doc.getFieldValue("score");
					progWeights.put(program, progScore / maxScore);
				}
			}
		}
		
		List<Result> queryResults = new ArrayList<Result>();
		
		// Query for keywords within each program.
		for (Map.Entry<String, Float> entry : progWeights.entrySet()) {
			String program = entry.getKey();
			Float progWeight = entry.getValue();
			Float progLength = progLengths.get(program);
			
			//if (progWeight < 0.5) continue;
			
			query = "{!df=phrase} type:trans AND program:" +
					program + " AND (" + q.getQueryText() + ")";
			
			try {
				results = SearchUtils.queryServer(server, query, 1000);
			} catch (SolrServerException e) {
				e.printStackTrace();
				return null;
			}
			
			/*maxScore = results.getMaxScore();
			
			// Convert results.
			List<Result> progResults = new ArrayList<Result>();
			
			for (SolrDocument doc : results) {
				Float start = (Float) doc.getFieldValue("start");
				Float end = (Float) doc.getFieldValue("end");
				Float docScore = (Float) doc.getFieldValue("score");
				
				// Program weight has more value than transcript weight.
				Float confidence = (float) (((progWeight) + 0.75*(docScore / maxScore)) / 1.75);
				
				progResults.add(new Result(program, start, end, start, confidence));
			}*/
			
			// Convert SolrDocument results to Results objects, calculating 
			// average cosine similarity as weighting.
			List<Result> progResults = new ArrayList<Result>();
			
			CosineDistanceMetric distanceMetric = new CosineDistanceMetric();
			
			SolrDocument[] resultsArray = results.toArray(new SolrDocument[0]);
			for (int i = 0; i < resultsArray.length; i++) {
				SolrDocument doc = resultsArray[i];
				
				Float start = (Float) doc.getFieldValue("start");
				Float end = (Float) doc.getFieldValue("end");
				String id = (String) doc.getFieldValue("id");
				
				MapVector docVector;
				try {
					docVector = SearchUtils.getTermVector(server, id);
				} catch (SolrServerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
				
				double cosine = 0.0;
				
				for (int j = 0; j < resultsArray.length; j++) {
					SolrDocument otherDoc = resultsArray[j];
					
					if (!doc.equals(otherDoc)) {
						System.out.println(i + ", " + j);
						
						String otherId = (String) otherDoc.getFieldValue("id");
						
						MapVector otherDocVector;
						try {
							otherDocVector = SearchUtils.getTermVector(server, otherId);
						} catch (SolrServerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return null;
						}
						
						cosine += distanceMetric.evaluate(docVector, otherDocVector);
					}
				}
				
				cosine /= results.size() - 1;
				
				progResults.add(new Result(program, start, end, start,
											progWeight * (float) cosine));
			}
			
			/*int[][] clusterParams = { { 10,  3 },
									  { 300, 2 },
									 // { 600, 2 },
									//  { 300, 3 }
									};

			for (int i = 0; i < clusterParams.length && progResults.size() > 2; i ++) {
				progResults = DataUtils.clusterResults(progResults,
													   clusterParams[i][0],
													   clusterParams[i][1],
													   true,
													   progLength);
			}*/
			
			// Cluster.
			@SuppressWarnings("serial")
			ClusterToClusterDivergenceFunction<DefaultCluster<Result>, Result> divFunc = 
					new ClusterToClusterDivergenceFunction<DefaultCluster<Result>, Result>() {

						@Override
						public double evaluate(DefaultCluster<Result> first,
								DefaultCluster<Result> second) {
							Result firstRes = DataUtils.clusterToResult(first);
							Result secondRes = DataUtils.clusterToResult(second);
							
							return firstRes.distanceTo(secondRes);
						}
						
						public CloneableSerializable clone() {
							return null;
						}
				
			};
			
			AgglomerativeClusterer<Result, DefaultCluster<Result>> clusterer = 
				new AgglomerativeClusterer<Result, DefaultCluster<Result>>(divFunc, new DefaultClusterCreator<Result>());
			ClusterHierarchyNode<Result, DefaultCluster<Result>> root = 
				clusterer.clusterHierarchically(progResults);
			
			// Create results from clusters.
			Queue<ClusterHierarchyNode<Result, DefaultCluster<Result>>> nodeQueue =
				new ConcurrentLinkedQueue<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
			nodeQueue.add(root);
			
			Set<Result> clusterResults = new HashSet<Result>();
			
			while (!nodeQueue.isEmpty()) {
				ClusterHierarchyNode<Result, DefaultCluster<Result>> cur =
					nodeQueue.poll();
				Result clusterResult = DataUtils.clusterToResult(cur);
				
				if (clusterResult.getConfidenceScore() > 0.1 &&
					clusterResult.getLength() > 150 &&
					clusterResult.getLength() < 0.5 * progLength) {
						clusterResults.add(clusterResult);
				}
				
				if (cur.getChildren() != null) {
					nodeQueue.addAll(cur.getChildren());
				}
			}
			
			/*// Junk results that are too short.
			progResults = FilterUtils.filter(progResults, new Predicate<Result>() {

				@Override
				public boolean test(Result object) {
					return object.getLength() > 1;
				}
				
			});*/
			
			queryResults.addAll(clusterResults);
		}
		
		// Order results by time and then by program.
		Collections.sort(queryResults, new DataUtils.ResultTimeComparator());
		Collections.sort(queryResults, new DataUtils.ResultProgramComparator());
		
		return queryResults;
	}

}
