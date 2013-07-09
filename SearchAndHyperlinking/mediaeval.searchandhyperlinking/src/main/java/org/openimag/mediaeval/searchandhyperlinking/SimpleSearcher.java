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
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.FVComparator;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.NearestNeighboursFactory;
import org.openimaj.math.geometry.shape.Circle;
import org.openimaj.ml.clustering.assignment.hard.ExactFloatAssigner;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.xml.sax.SAXException;

public class SimpleSearcher implements Searcher {
	private SolrServer server;
	
	public SimpleSearcher(SolrServer server) {
		this.server = server;
	}

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		SimpleSearcher ss = new SimpleSearcher(new HttpSolrServer("http://seurat:8983/solr/"));
		
		List<Query> queries = Query.readQueriesFromFile(new File("/home/jpreston/Work/data/mediaeval/mediaeval-searchhyper/dev/dev-queries.xml"));
		
		for (Query q : queries) {
			System.out.println(q);
			System.out.println("==========\n");
			
			List<Result> res = ss.search(q);
			
			for (Result r : res) {
				System.out.println(r);
			}
			
			System.out.println("----------\n\n");
		}
	}
	
	@Override
	public List<Result> search(Query q) {
		// Get results.
		SolrQuery solrQuery = new SolrQuery("phrase:" + q.getQueryText() + "");
		solrQuery.setRows(1000);
		
		QueryResponse response;
		try {
			response = server.query(solrQuery);
		} catch (SolrServerException e) {
			e.printStackTrace();
			return null;
		}
		
		SolrDocumentList results = response.getResults();
		
		// Group by program.
		Map<String, List<SolrDocument>> docsByProgram = new HashMap<String, List<SolrDocument>>();
		for (SolrDocument doc : results) {
			String program = (String) doc.getFieldValue("program");
			
			if (docsByProgram.containsKey(program)) {
				List<SolrDocument> set = docsByProgram.get(program);
				set.add(doc);
			} else {
				List<SolrDocument> set = new ArrayList<SolrDocument>();
				set.add(doc);
				docsByProgram.put(program, set);
			}
		}
		
		List<Result> queryResults = new ArrayList<Result>();
		
		// Cluster with DBScan within a program to find relevant clusters.
		for (Map.Entry<String, List<SolrDocument>> entry : docsByProgram.entrySet()){
			String program = entry.getKey();
			List<SolrDocument> docs = entry.getValue();
			
			SolrQuery sq = new SolrQuery();
			sq.setQuery("program:" + program + " AND type:progmeta");
			QueryResponse r;
			try {
				r = server.query(sq);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			SolrDocument progMetaDoc = r.getResults().get(0);
			
			Float length = (Float) progMetaDoc.getFieldValue("length");
			
			double[][] data = new double[docs.size()][2];
			for (int i = 0; i < docs.size(); i++) {
				data[i][0] = (Float) docs.get(i).getFieldValue("start");
				data[i][1] = (Float) docs.get(i).getFieldValue("end");
			}
			
			MBFImage vis = DataUtils.visualiseData(data, length, RGBColour.GREEN);
			
			NearestNeighboursFactory<DoubleNearestNeighboursExact, double[]> nnFactory =
				new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.EUCLIDEAN);
			DBSCANConfiguration<DoubleNearestNeighbours, double[]> dbscanConfig = 
				new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(2, 0.05 * length, 4, nnFactory);
			DoubleDBSCAN dbscan = new DoubleDBSCAN(dbscanConfig);
			
			DoubleDBSCANClusters clusters = dbscan.cluster(data);
			
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
				
				double[][] clusterData = { { min, max } };
				vis.addInplace(DataUtils.visualiseData(clusterData, length, RGBColour.RED));
				clustered = true;
				
				Result clusterResult =
					new Result(program, (float) min, (float) max, (float) min, indices[i].length);
				queryResults.add(clusterResult);
			}
			
			if (clustered) DisplayUtilities.display(vis);
		}
		
		Collections.sort(queryResults, new Comparator<Result>() {

			@Override
			public int compare(Result arg0, Result arg1) {
				return Math.round(arg1.getConfidenceScore() - arg0.getConfidenceScore());
			}
		
		});
		
		return queryResults;
	}
	
	

}
