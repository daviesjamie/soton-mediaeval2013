package org.openimag.mediaeval.searchandhyperlinking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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
				
				if (r.getConfidenceScore() > 0.35) {
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
	}
	
	public ProgramRestrictedTextSearcher(SolrServer server) {
		this.server = server;
	}

	@Override
	public List<Result> search(Query q) {
		DataUtils.cleanQuery(q);
		
		// Maximum number of programs to query in synopsis for.
		final int MAX_SYNOPSIS_RESULTS = 3;
		
		// Stores weight for each program.
		Map<String, Float> progWeights = new HashMap<String, Float>();
		Map<String, Float> progLength = new HashMap<String, Float>();
		
		// Get synopsis results.
		SolrQuery solrQuery = new SolrQuery("{!df=synopsis} type:progmeta AND (" + 
											q.getQueryText() + ")");
		solrQuery.setRows(MAX_SYNOPSIS_RESULTS);
		solrQuery.addField("score");
		solrQuery.addField("*");
		
		QueryResponse response;
		try {
			response = server.query(solrQuery);
		} catch (SolrServerException e) {
			e.printStackTrace();
			return null;
		}
		
		Set<String> progNames = new HashSet<String>();
		
		// Weight programs and add to list, add titles to progName set.
		SolrDocumentList results = response.getResults();
		
		float maxScore = results.getMaxScore();
		
		for (int i = 0; i < results.size(); i++) {
			String program = (String) results.get(i).getFieldValue("program");
			float progScore = (Float) results.get(i).getFieldValue("score");
			progWeights.put(program, progScore / maxScore);
			progLength.put(program, (Float) results.get(i).getFieldValue("length"));
			progNames.add((String) results.get(i).getFieldValue("title"));
		}
		
		// Find program IDs matching titles from synopses, add to list.
		for (String title : progNames) {
			solrQuery = new SolrQuery("{!df=title} type:progmeta AND \"" + title + "\"");
			solrQuery.setRows(100);
			solrQuery.addField("score");
			solrQuery.addField("*");
			
			try {
				response = server.query(solrQuery);
			} catch (SolrServerException e) {
				e.printStackTrace();
				return null;
			}
			
			results = response.getResults();
			
			// Scale maxScore so that results are skewed in favour of synopsis
			// hits.
			maxScore = (float) (results.getMaxScore() * 1.5);
			
			for (SolrDocument doc : results) {
				String program = (String) doc.getFieldValue("program");
				
				progLength.put(program, (Float) doc.getFieldValue("length"));
				
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
			
			if (progWeight < 0.5) continue;
			
			solrQuery = new SolrQuery("{!df=phrase} type:trans AND program:" +
									  program + " AND (" + q.getQueryText() + ")");
			solrQuery.setRows(1000);
			
			try {
				response = server.query(solrQuery);
			} catch (SolrServerException e) {
				e.printStackTrace();
				return null;
			}
		
			results = response.getResults();
			
			// Cluster results.
			double[][] data = new double[results.size()][2];
			for (int i = 0; i < results.size(); i++) {
				data[i][0] = (Float) results.get(i).getFieldValue("start");
				data[i][1] = (Float) results.get(i).getFieldValue("end");
			}
			
			MBFImage vis = DataUtils.visualiseData(data, progLength.get(program), RGBColour.GREEN);
			
			NearestNeighboursFactory<DoubleNearestNeighboursExact, double[]> nnFactory =
				new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.EUCLIDEAN);
			DBSCANConfiguration<DoubleNearestNeighbours, double[]> dbscanConfig = 
				new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(2, 10, 3, nnFactory);
			DoubleDBSCAN dbscan = new DoubleDBSCAN(dbscanConfig);
			
			DoubleDBSCANClusters clusters = dbscan.cluster(data);
			
			List<Result> progResults = new ArrayList<Result>();
			
			float maxConf = 0;
			
			boolean clustered = false;
			
			int[][] indices = clusters.getClusterMembers();
			for (int i = 0; i < indices.length; i++) {
				double min = Double.MAX_VALUE;
				double max = 0;
				
				for (int j = 0; j < indices[i].length; j++) {
					double start = data[indices[i][j]][0];
					double end = data[indices[i][j]][1];
					
					if (start < min) min = start;
					if (end > max) max = end;
				}
				
				double[][] minMaxData = { { min, max } };
				
				vis.addInplace(
					DataUtils.visualiseData(minMaxData,
											progLength.get(program),
											RGBColour.RED));
				clustered = true;
				
				if (maxConf < indices[i].length) maxConf = indices[i].length;
				
				Result clusterResult =
					new Result(program, (float) min, (float) max, (float) min, indices[i].length);
				progResults.add(clusterResult);
			}
			
			// Scale confidences by maxConf and progWeight.
			for (Result res : progResults) {
				res.setConfidenceScore(progWeight * res.getConfidenceScore() / maxConf);
			}
			
			queryResults.addAll(progResults);
			
			if (clustered) DisplayUtilities.displayName(vis, q.getQueryText() + ": " + program);
		}
		
		Collections.sort(queryResults, new Comparator<Result>() {

			@Override
			public int compare(Result arg0, Result arg1) {
				float diff = arg1.getConfidenceScore() - arg0.getConfidenceScore();
				
				if (diff < 0) {
					return -1;
				} else if (diff == 0) {
					return 0;
				} else {
					return 1;
				}
			}
		
		});
		
		return queryResults;
	}

}
