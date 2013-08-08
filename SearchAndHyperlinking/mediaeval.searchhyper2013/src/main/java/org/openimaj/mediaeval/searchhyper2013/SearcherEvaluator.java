package org.openimaj.mediaeval.searchhyper2013;

import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.mtj.DenseVectorFactoryMTJ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class SearcherEvaluator {
	private Searcher searcher;
	
	public SearcherEvaluator(Searcher searcher) {
		this.searcher = searcher;
	}

	public static Map<Query, Set<Result>> importExpected(File queryFile,
														 File qRelFile)
				throws IOException, ParserConfigurationException, SAXException {
		// First get queries.
		Map<String, Query> queries = new HashMap<String, Query>();
		
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document document = builder.parse(queryFile);
		
		Element root = document.getDocumentElement();
		
		NodeList segmentNodeList = root.getChildNodes();
		for (int j = 0; j < segmentNodeList.getLength(); j++) {
			if (segmentNodeList.item(j) instanceof Element) {
				Element segmentElement = (Element) segmentNodeList.item(j);
				
				if (segmentElement.getTagName().equals("top")) {
					NodeList wordNodeList = segmentElement.getChildNodes();
					
					String itemId = null;
					String queryText = null;
					String visualQueues = null;
					
					for (int k = 0; k < wordNodeList.getLength(); k++) {
						if (wordNodeList.item(k) instanceof Element) {
							Element wordElement = (Element) wordNodeList.item(k);
							
							if (wordElement.getTagName().equals("itemId")) {
								itemId = wordElement.getTextContent();
							} else if (wordElement.getTagName().equals("queryText")) {
								queryText = wordElement.getTextContent();
							} else if (wordElement.getTagName().equals("visualQueues")) {
								visualQueues = wordElement.getTextContent();
							}
						}
					}
					
					queries.put(itemId, new Query(itemId, queryText, visualQueues));
				}
			}
		}
		
		// Next get results.
		Map<Query, Set<Result>> queryResults = new HashMap<Query, Set<Result>>();
		 
		String[] qRelLines = FileUtils.readlines(qRelFile);
		
		for (String line : qRelLines) {
			String[] parts = line.split(" ");
			
			Result result = new Result();
			
			if (parts.length == 4) {
				result.fileName = parts[1];
				result.startTime = Float.parseFloat(parts[2]);
				result.endTime = Float.parseFloat(parts[3]);
			} else if (parts.length == 6) {
				result.fileName = parts[1];
				result.startTime = Float.parseFloat(parts[2]);
				result.endTime = Float.parseFloat(parts[3]);
			    result.jumpInPoint = Float.parseFloat(parts[4]);
			    result.confidenceScore = Float.parseFloat(parts[5]);
			} else {
				return null;
			}
			
			Set<Result> results = queryResults.get(queries.get(parts[0]));
			
			if (results == null) {
				results = new HashSet<Result>();
				results.add(result);
				queryResults.put(queries.get(parts[0]), results);
			} else {
				results.add(result);
			}
		}
		
		return queryResults;
	}
	
	public Vector evaluateAgainstExpectedResults(
						Map<Query, Set<Result>> expectedResults,
						int windowSize) {
		final int GRANULARITY = 10;
		
		List<Float> rr = new ArrayList<Float>();
		List<Float> gap = new ArrayList<Float>();
		List<Float> asp = new ArrayList<Float>();
		
		for (Query query : expectedResults.keySet()) {			
			String fileName = expectedResults.get(query).iterator().next().fileName;
			float qRelStart = expectedResults.get(query).iterator().next().startTime;
			float qRelEnd = expectedResults.get(query).iterator().next().endTime;
			
			float totalLen = 0;
			boolean relFlag = false;
			
			List<Result> runResults;
			try {
				runResults = searcher.search(query);
			} catch (SearcherException e) {
				e.printStackTrace();
				continue;
			}
			
			System.out.println("\nResults: \n" + runResults);
			
			for (int i = 0; i < runResults.size(); i++) {
				Result result = runResults.get(i);
				
				totalLen += result.endTime - result.startTime;
				
				if (("v" + result.fileName).equals(fileName)) {
					if (qRelStart - windowSize <= result.jumpInPoint &&
						result.jumpInPoint <= qRelEnd + windowSize) {
							relFlag = true;
							rr.add(1f / (i + 1));
							
							float penalty = 0;
							if (result.jumpInPoint <= qRelStart) {
								penalty = (float) Math.ceil((qRelStart - result.jumpInPoint) / GRANULARITY);
							} else if (qRelStart < result.jumpInPoint) {
								penalty = (float) Math.ceil((result.jumpInPoint - qRelStart) / GRANULARITY);
							}
							
							gap.add((1 - (penalty * GRANULARITY / windowSize)/(i+1)));
							
							float relLen = 0;
							if (qRelStart <= result.startTime && result.endTime <= qRelEnd) {
								relLen = result.endTime - result.startTime;
							} else if (result.startTime < qRelStart && qRelEnd < result.endTime) {
								relLen = qRelEnd - qRelStart;
							} else if (result.startTime < qRelStart && qRelStart < result.endTime && result.endTime < qRelEnd) {
								relLen = result.endTime - qRelStart;
							} else if (qRelStart < result.startTime && result.startTime < qRelEnd && qRelEnd < result.endTime) {
								relLen = qRelEnd - result.startTime;
							}
							
							if (relLen != 0) {
								asp.add(relLen / totalLen);
							} else {
								asp.add(0f);
							}
					}
				}
			}
			
			if (relFlag == false) {
				rr.add(0f);
				gap.add(0f);
				asp.add(0f);
			}
		}
		
		float mrr = 0;
		float mgap = 0;
		float masp = 0;
		
		for (int i = 0; i < rr.size(); i++) {
			mrr += rr.get(i);
			mgap += gap.get(i);
			masp += asp.get(i);
		}
		
		mrr /= rr.size();
		mgap /= rr.size();
		masp /= rr.size();
		
		DenseVectorFactoryMTJ vectorFactory = new DenseVectorFactoryMTJ();
		Vector resultVector = vectorFactory.copyValues(mrr, mgap, masp);
		
		return resultVector;
	}
}
