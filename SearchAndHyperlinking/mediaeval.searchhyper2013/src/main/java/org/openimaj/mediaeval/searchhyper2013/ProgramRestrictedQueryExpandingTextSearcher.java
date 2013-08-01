package org.openimaj.mediaeval.searchhyper2013;

import gov.sandia.cognition.learning.algorithm.clustering.AgglomerativeClusterer;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.Cluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultClusterCreator;
import gov.sandia.cognition.learning.algorithm.clustering.divergence.ClusterToClusterDivergenceFunction;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;
import gov.sandia.cognition.learning.function.distance.CosineDistanceMetric;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.util.CloneableSerializable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import com.google.common.collect.Collections2;
import com.google.common.collect.Constraint;
import com.google.common.collect.Constraints;

/**
 * Works like a ProgramREstrictedTextSearcher but also has a query expansion 
 * step within each program.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class ProgramRestrictedQueryExpandingTextSearcher implements Searcher {
	private int MAX_SYNOPSIS_RESULTS = 3;
	private float TITLE_WEIGHT_THRESHOLD = 2;
	private int MAX_TITLE_RESULTS = 10;
	private float TITLE_RESULT_SCALE_FACTOR = 0.5f;
	private int MAX_TRANS_RESULTS = 500;
	private int MAX_EXPAND_RESULTS = 5;
	private float EXPAND_RESULT_SCALE_FACTOR = 5;
	
	private SolrServer server;
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		SolrServer serv = new HttpSolrServer("http://seurat:8983/solr");
		ProgramRestrictedQueryExpandingTextSearcher searcher = new ProgramRestrictedQueryExpandingTextSearcher(serv);
		
		List<Query> queries = Query.readQueriesFromFile(new File("/home/jpreston/Work/data/mediaeval/mediaeval-searchhyper/dev/dev-queries.xml"));
		
		for (int i = 0; i < queries.size(); i++) {
			Query q = queries.get(i);
			List<Result> res = searcher.search(q);
		
			for (int j = 0; j < res.size(); j++) {
				Result r = res.get(j);
				
				System.out.println(q.getQueryID() + " " +
								  "Q0" + " " +
								  "v" + r.getProgram() + " " +
								  DataUtils.StoMS(r.getStartTime()) + " " +
								  DataUtils.StoMS(r.getEndTime()) + " " +
								  DataUtils.StoMS(r.getJumpInPoint()) + " " +
								  (j + 1) + " " +
								  r.getConfidenceScore() + " " +
								  "run_1");
			}
		}
	}
	
	public ProgramRestrictedQueryExpandingTextSearcher(SolrServer server) {
		this.server = server;
	}

	@Override
	public List<Result> search(Query q) {		
		String query = q.getQueryText();
		
		SolrDocumentList synopsisSolrResults;
		try {
			synopsisSolrResults =
				SearchUtils.queryServer(server,
										"{!df=synopsis} (" + query + ")",
										"type:progmeta",
										MAX_SYNOPSIS_RESULTS);
		} catch (SolrServerException e) {
			e.printStackTrace();
			return null;
		}
		
		// Store each synopsis result as a program to query within, with a 
		// normalised program weight. Also extract program titles with weight.
		// Also store lengths.
		Map<String, Float> programsWithWeights = new HashMap<String, Float>();
		Map<String, Float> titlesWithWeights = new HashMap<String, Float>();
		Map<String, Float> programsWithLengths = new HashMap<String, Float>();
		
		for (SolrDocument result : synopsisSolrResults) {
			String program = (String) result.getFieldValue("program");
			String title = (String) result.getFieldValue("title");
			float weight = ((Float) result.getFieldValue("score")) + 1;
			float length = (Float) result.getFieldValue("length");
			
			programsWithWeights.put(program, weight);
			titlesWithWeights.put(title, weight);
			programsWithLengths.put(program, length);
		}
		
		// For each title, if the title weight exceeds a threshold, find other 
		// programs with that title and add them to programsWithWeights, 
		// scaling the weight.
		
		for (String title : titlesWithWeights.keySet()) {
			float weight = titlesWithWeights.get(title);
			
			if (weight > TITLE_WEIGHT_THRESHOLD) {
				SolrDocumentList titleSolrResults;
				try {
					titleSolrResults =
						SearchUtils.queryServer(server,
												"{!df=title} \"" + 
													title + "\"",
												"type:progmeta",
												MAX_TITLE_RESULTS);
				} catch (SolrServerException e) {
					e.printStackTrace();
					return null;
				}
				
				for (SolrDocument result : titleSolrResults) {
					String program = (String) result.getFieldValue("program");
					float length = (Float) result.getFieldValue("length");
					
					if (!programsWithWeights.containsKey(program)) {
						programsWithWeights.put(program,
												TITLE_RESULT_SCALE_FACTOR * weight);
						programsWithLengths.put(program, length);
					}
				}
			}
		}
		
		// Generate results for each program: create Result objects from 
		// transcript hits and cluster into bigger Results.
		
		List<Result> queryResults = new ArrayList<Result>();
		
		for (String program : programsWithWeights.keySet()) {
			// Query.
			SolrDocumentList transSolrResults;
			try {
				transSolrResults =
					SearchUtils.queryServer(server,
											"{!df=phrase} (" + query + ")",
											"type:trans AND program:" + program,
											MAX_TRANS_RESULTS);
			} catch (SolrServerException e) {
				e.printStackTrace();
				return null;
			}
			
			// Expand query by re-searching with all results from previous 
			// query.
			
			String expansionQuery = DataUtils.createExpansionQuery(transSolrResults);
			
			//System.out.println(program + " " + expansionQuery);
			
			SolrDocumentList expansionSolrResults;
			try {
				expansionSolrResults =
					SearchUtils.queryServer(server,
											"{!df=phrase} " + expansionQuery,
											"type:trans AND program:" + program,
											MAX_EXPAND_RESULTS);
			} catch (SolrServerException e) {
				e.printStackTrace();
				return null;
			}
			
			for (SolrDocument result : expansionSolrResults) {
				result.setField("score", EXPAND_RESULT_SCALE_FACTOR * ((Float) result.getFieldValue("score")));
			}
			
			//DisplayUtilities.displayName(DataUtils.visualiseData(expansionSolrResults, 200, programsWithLengths.get(program), RGBColour.RED), program);
			
			transSolrResults.addAll(expansionSolrResults);
			
			// Create Result objects.
			List<Result> transResults = new ArrayList<Result>();
			
			for (SolrDocument result : transSolrResults) {
				float start = (Float) result.getFieldValue("start");
				float end = (Float) result.getFieldValue("end");
				float weight = (Float) (result.getFieldValue("score")) + 1;

				transResults.add(new Result(program, start, end, start, weight));
			}
			
			// Cluster Results.
			@SuppressWarnings("serial")
			ClusterToClusterDivergenceFunction<DefaultCluster<Result>, Result> divFunc = 
				new ClusterToClusterDivergenceFunction<DefaultCluster<Result>, Result>() {

					@Override
					public double evaluate(DefaultCluster<Result> first,
							DefaultCluster<Result> second) {
						Result firstRes = DataUtils.clusterToResult(first);
						Result secondRes = DataUtils.clusterToResult(second);
						
						double absConfDiff = Math.abs(firstRes.getConfidenceScore() -
													 secondRes.getConfidenceScore());
						float tempDist = firstRes.distanceTo(secondRes);
						
						//return absConfDiff;
						//return tempDist;
						return (tempDist * absConfDiff) / (tempDist + absConfDiff + 1);
					}
					
					public CloneableSerializable clone() {
						return null;
					}
			
			};
			
			AgglomerativeClusterer<Result, DefaultCluster<Result>> clusterer = 
				new AgglomerativeClusterer<Result, DefaultCluster<Result>>(divFunc, new DefaultClusterCreator<Result>());
			ClusterHierarchyNode<Result, DefaultCluster<Result>> root = 
				clusterer.clusterHierarchically(transResults);

			// Create results from clusters, constraining such that the 
			// temporally largest Result is the one in the set at the end.
			Queue<ClusterHierarchyNode<Result, DefaultCluster<Result>>> nodeQueue =
				new ConcurrentLinkedQueue<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
			nodeQueue.add(root);
			
			class TemporalException extends RuntimeException {
				
			}
			
			class TemporalResultSetConstraint implements Constraint<Result> {
				Set<Result> resultSet;
				
				public TemporalResultSetConstraint(Set<Result> resultSet) {
					this.resultSet = resultSet;
				}
				
				@Override
				public Result checkElement(Result element) {
					float start = element.getStartTime();
					float end = element.getEndTime();

					Iterator<Result> iter = resultSet.iterator();
					while (iter.hasNext()) {
						Result result = iter.next();
						
						float resultStart = result.getStartTime();
						float resultEnd = result.getEndTime();
						
						if (start <= resultStart && end >= resultEnd) {
							iter.remove();
						} else if (start >= resultStart && end <= resultEnd) {
							throw new TemporalException();
						}
					}
					
					return element;
				}
			
			}
			
			Set<Result> clusterResults = new HashSet<Result>();
			clusterResults =
				Constraints.constrainedSet(clusterResults,
										   new TemporalResultSetConstraint(clusterResults));

			while (!nodeQueue.isEmpty()) {
				ClusterHierarchyNode<Result, DefaultCluster<Result>> cur =
					nodeQueue.poll();
				Result clusterResult = DataUtils.clusterToResult(cur);
				
				if (clusterResult.getLength() > 60 * 5 &&
					clusterResult.getLength() < 0.25 * programsWithLengths.get(program)) {
						try {
							clusterResults.add(clusterResult);
						} catch (TemporalException e) {
							continue;
						}
				}
				
				if (cur.getChildren() != null) {
					nodeQueue.addAll(cur.getChildren());
				}
			}
			
			// Scale Result weights by program weight.
			for (Result result : clusterResults) {
				result.setConfidenceScore(
					(result.getConfidenceScore() *
					programsWithWeights.get(program)));
			}
			
			queryResults.addAll(clusterResults);
		}
		
		if (queryResults.size() == 0) return null;
		
		// Sort by confidence.
		Collections.sort(queryResults, new Result.ResultConfidenceComparator());
		
		// Filter by confidence.
		final float maxConf = queryResults.get(0).getConfidenceScore();

		queryResults = FilterUtils.filter(queryResults, new Predicate<Result>() {

			@Override
			public boolean test(Result object) {
				return object.getConfidenceScore() > 0.1 * maxConf;
			}
			
		});
		
		return queryResults;
	}

	@Override
	public void setProperties(Vector input) {
		MAX_SYNOPSIS_RESULTS = (int) input.getElement(0);
		TITLE_WEIGHT_THRESHOLD = (float) input.getElement(1);
		MAX_TITLE_RESULTS = (int) input.getElement(2);
		TITLE_RESULT_SCALE_FACTOR = (float) input.getElement(3);
		MAX_TRANS_RESULTS = (int) input.getElement(4);
		MAX_EXPAND_RESULTS = (int) input.getElement(5);
		EXPAND_RESULT_SCALE_FACTOR = (float) input.getElement(6);
	}

}
