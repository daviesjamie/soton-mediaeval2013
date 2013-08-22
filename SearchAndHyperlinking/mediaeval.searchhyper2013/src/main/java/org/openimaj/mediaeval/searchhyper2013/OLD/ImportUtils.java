package org.openimaj.mediaeval.searchhyper2013.OLD;

import gov.sandia.cognition.learning.algorithm.clustering.AgglomerativeClusterer;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.Cluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultClusterCreator;
import gov.sandia.cognition.learning.algorithm.clustering.divergence.ClusterToClusterDivergenceFunction;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;
import gov.sandia.cognition.util.CloneableSerializable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.openimaj.data.AbstractDataSource;
import org.openimaj.data.DataSource;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.Transforms;
import org.openimaj.io.FileUtils;
import org.openimaj.io.IOUtils;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

/**
 * Contains various utility methods for importing data into Solr.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public abstract class ImportUtils {
	
	public static List<SolrInputDocument> readSubtitlesFile(File subsFile)
														   throws SAXException, FileNotFoundException, IOException {
		System.out.println("(Subs) " + subsFile.getName());
		
		XMLReader xr = XMLReaderFactory.createXMLReader();
		
		final List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		final String progName = subsFile.getName().split("\\.")[0];
		
		xr.setContentHandler(new DefaultHandler() {
			SolrInputDocument currentDoc = null;
			int count = 0;
			
			public void startElement(String uri,
									 String localName,
									 String qName,
	                				 Attributes attributes)
	                						 throws SAXException {
				if (localName.equals("p") && currentDoc == null) {
					currentDoc = new SolrInputDocument();
					
					currentDoc.addField("id", progName + "_trans_subtitles_" + count);
					currentDoc.addField("type", "trans");
					currentDoc.addField("program", progName);
					currentDoc.addField("start", DataUtils.HMStoS(attributes.getValue("begin")));
					currentDoc.addField("end", DataUtils.HMStoS(attributes.getValue("end")));
					currentDoc.addField("source", "subtitles");
					
					count++;
				}
			}
			
			public void characters(char[] ch,
								   int start,
								   int length)
										   throws SAXException {
				if (currentDoc != null) {
					currentDoc.addField("phrase",
										String.valueOf(ch, start, length));
					docs.add(currentDoc);
					
					currentDoc = null;
				}
			}
		});
		
		xr.parse(new InputSource(new FileReader(subsFile)));

		return docs;
	}
	
	public static List<SolrInputDocument> readLIMSIFile(File limsiFile)
			throws FileNotFoundException, IOException, ParserConfigurationException, SAXException {
		System.out.println("(LIMSI) " + limsiFile.getName());
		
		String progName = limsiFile.getName().split("\\.")[0];
		int count = 0;
		
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document document = builder.parse(limsiFile);
		
		Element root = document.getDocumentElement();
		
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		// Iterate over top-level nodes.
		NodeList nodeList = root.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element) {
				Element element = (Element) nodeList.item(i);
				
				// We're going into a SegmentList.
				if (element.getTagName().equals("SegmentList")) {
					NodeList segmentNodeList = element.getChildNodes();
					for (int j = 0; j < segmentNodeList.getLength(); j++) {
						if (segmentNodeList.item(j) instanceof Element) {
							Element segmentElement = (Element) segmentNodeList.item(j);
							
							// We've got a Segment.
							if (segmentElement.getTagName().equals("SpeechSegment")) {
								
								// Set up doc properties.
								SolrInputDocument solrDoc = new SolrInputDocument();
								
								solrDoc.addField("id", progName + "_trans_limsi_" + count);
								solrDoc.addField("type", "trans");
								solrDoc.addField("program", progName);
								solrDoc.addField("start", Float.parseFloat(
															segmentElement.getAttribute(
																"stime")));
								solrDoc.addField("end", Float.parseFloat(
															segmentElement.getAttribute(
																"etime")));
								solrDoc.addField("source", "limsi");
								
								count++;
								
								SortedMap<Float, SortedMap<Float, String>> transcriptMatrix = 
										new TreeMap<Float, SortedMap<Float, String>>();
								
								NodeList wordNodeList = segmentElement.getChildNodes();
								for (int k = 0; k < wordNodeList.getLength(); k++) {
									if (wordNodeList.item(k) instanceof Element) {
										Element wordElement = (Element) wordNodeList.item(k);
										
										// Get each Word and group together words occurring
										// at same time (alternatives), maintaining 
										// confidence information.
										if (wordElement.getTagName().equals("Word")) {
											String word = wordElement.getTextContent();
											
											if (word.equals(" {fw} ")) {
												continue;
											}
											
											Float start = Float.parseFloat(
															wordElement.getAttribute("stime"));
											Float conf = Float.parseFloat(
															wordElement.getAttribute("conf"));
											
											SortedMap<Float, String> words =
													transcriptMatrix.get(start);
											
											if (words != null) {
												words.put(conf, word);
											} else {
												words = new TreeMap<Float, String>();
												words.put(conf, word);
												transcriptMatrix.put(start, words);
											}
										}
									}
								}
								
								// Generate most probable transcript from the
								// confidence scores.
								String phrase = "";
								
								for (SortedMap<Float, String> pos : 
										transcriptMatrix.values()) {
									String next = "";
									
									for (String s : pos.values()) {
										next = s;
									}
									
									phrase += next;
								}
								
								solrDoc.addField("phrase", phrase);
								docs.add(solrDoc);
							}
						}
					}
				}
			}
		}
		
		return docs;
	}
    
    public static List<SolrInputDocument> readLIUMFile(File liumFile) throws IOException {
        System.out.println("(LIUM) " + liumFile.getName());
        
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        
        String[] lines = FileUtils.readlines(liumFile);
       
        if (lines.length == 0) {
        	return docs;
        }
       
        String prog = lines[0].split(" ", 2)[0];
        
        final List<LIUMWord> words = new ArrayList<LIUMWord>();
        
        for (String line : lines) {
        	String[] components = line.split(" ");
        	
        	LIUMWord liumWord = new LIUMWord();
        	liumWord.start = Float.parseFloat(components[2]);
        	liumWord.end = liumWord.start;
        	liumWord.word = components[4];
        	liumWord.confidence = Float.parseFloat(components[5]);
        	
        	words.add(liumWord);
        }
        
        /*DataSource<double[]> liumWordStartTimeDataSource = new AbstractDataSource<double[]>() {

			@Override
			public void getData(int startRow, int stopRow, double[][] data) {
				for (int i = startRow; i < stopRow; i++) {
					data[i][0] = words.get(i).start;
				}
			}

			@Override
			public double[] getData(int row) {
				double[] arr = new double[1];
				arr[0] = words.get(row).start;
				return arr;
			}

			@Override
			public int numDimensions() {
				return 1;
			}

			@Override
			public int numRows() {
				return words.size() - 1;
			}

        };
        
        DBSCANConfiguration<DoubleNearestNeighbours, double[]> dbscanConfig = 
        	new DBSCANConfiguration<DoubleNearestNeighbours, double[]>
        		(1, 1.5, 2, new DoubleNearestNeighboursExact.Factory());
        DoubleDBSCAN dbscan = new DoubleDBSCAN(dbscanConfig);
        
        DoubleDBSCANClusters dbscanClusters = dbscan.cluster(liumWordStartTimeDataSource);
        
        int[][] clusters = dbscanClusters.clusters();
        
        for (int i = 0; i < clusters.length; i++) {
        	Arrays.sort(clusters[i]);
        	
        	Float start = Float.MAX_VALUE;
        	Float end = 0f;
        	Float confidence = 0f;
        	String phrase = "";
        	
        	for (int j = 0; j < clusters[i].length; j++) {
        		LIUMWord word = words.get(clusters[i][j]);
        		
        		if (start > word.start) {
        			start = word.start;
        		}
        		
        		if (end < word.start) {
        			end = word.start;
        		}
        		
        		confidence += word.confidence;
        		
        		phrase += word.word + " ";
        	}
        	
        	confidence /= clusters[i].length;
        	
        	SolrInputDocument doc = new SolrInputDocument();
        	doc.addField("id", prog + "_trans_lium_" + i);
        	doc.addField("type", "trans");
        	doc.addField("program", prog);
        	doc.addField("start", start);
        	doc.addField("end", end);
        	doc.addField("source", "lium");
        	doc.addField("phrase", phrase);
        	
        	docs.add(doc);
        }*/
        
        ClusterToClusterDivergenceFunction<DefaultCluster<LIUMWord>, LIUMWord> divFunc = 
        	new ClusterToClusterDivergenceFunction<DefaultCluster<LIUMWord>, LIUMWord>() {

				@Override
				public double evaluate(DefaultCluster<LIUMWord> first,
						DefaultCluster<LIUMWord> second) {
					LIUMWord firstWord = LIUMWord.collectionToLIUMWord(first.getMembers());
					LIUMWord secondWord = LIUMWord.collectionToLIUMWord(second.getMembers());
					
					/*if (firstWord.start < secondWord.start) {
						if (firstWord.end < secondWord.start) {
							return secondWord.start - firstWord.end;
						} else {
							return 0;
						}
					} else {
						if (secondWord.end < firstWord.start) {
							return firstWord.start - secondWord.end;
						} else {
							return 0;
						}
					}*/
					
					return Math.pow(firstWord.start - secondWord.start, 2) +
						   Math.pow(firstWord.end - secondWord.end, 2);
				}
				
				public CloneableSerializable clone() {
					return null;
				}
        	
        };
        
        AgglomerativeClusterer<LIUMWord, DefaultCluster<LIUMWord>> clusterer = 
        	new AgglomerativeClusterer<LIUMWord, DefaultCluster<LIUMWord>>
        		(divFunc, new DefaultClusterCreator<LIUMWord>());
        ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>> root = 
        	clusterer.clusterHierarchically(words);
        
        
        
        
        
                
        
        int maxDepth = DataUtils.clusterHierarchyDepth(root);
		
		int HEIGHT = 50;
		int Y_DIM = HEIGHT;
		int X_DIM = 800;
		float programLength = 30*60;
		
		MBFImage vis = new MBFImage(X_DIM, Y_DIM, ColourSpace.HSV);
		
		int depth = 0;
		
		List<ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>>> nextLevel = 
			new ArrayList<ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>>>();
		nextLevel.add(root);
	
		// We add child nodes for the current level to the nextLevel list so 
		// that we can process the tree in a breadth-first manner.
		while (!nextLevel.isEmpty()) {
			List<ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>>> newNextLevel =
				new ArrayList<ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>>>();
			
			for (ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>> node : nextLevel) {
				List<LIUMWord> thisLevel = new ArrayList<LIUMWord>(node.getMembers());
				
				if (node.getChildren() != null) {
					for (ClusterHierarchyNode<LIUMWord, DefaultCluster<LIUMWord>> child : node.getChildren()) {
						newNextLevel.add(child);
					}
				}
				
				LIUMWord clusterLIUMWord = LIUMWord.collectionToLIUMWord(thisLevel);
				
				int x0 = (int)((clusterLIUMWord.start / programLength) * X_DIM);
				int x1 = (int)((clusterLIUMWord.end / programLength) * X_DIM);
				int y = 0;
				Float[] col = { Math.min(depth * (1f / maxDepth), 1f), 1f, 1f };

				Rectangle rect = new Rectangle(x0, y, x1 - x0, HEIGHT - 2);
				vis.drawShapeFilled(rect, col);
			}
			
			nextLevel = newNextLevel;
			depth++;
		}
		
		DisplayUtilities.display(Transforms.HSV_TO_RGB(vis));
        
        
        
        
        
        return docs;
    }
    
    private static class LIUMWord {
       	Float start;
       	Float end;
       	String word;
       	Float confidence;
       	
       	private static LIUMWord collectionToLIUMWord(Collection<LIUMWord> collection) {
	   		LIUMWord word = new LIUMWord();
	    		
	   		word.start = Float.MAX_VALUE;
	   		word.end = 0f;
	   		word.word = "";
	   		word.confidence = 0f;
	   	
	   		List<LIUMWord> clusterWords = new ArrayList<LIUMWord>(collection);
	   		Collections.sort(clusterWords, new Comparator<LIUMWord>() {
	
				@Override
				public int compare(LIUMWord arg0, LIUMWord arg1) {
					if (arg1.start > arg0.start) {
						return 1;
					} else if (arg1.start < arg0.start) {
						return -1;
					} else {
						return 0;
					}
				}
	   			
	   		});
	   		
	   		for (LIUMWord clusterWord : clusterWords) {
	   			if (clusterWord.start < word.start) {
	   				word.start = clusterWord.start;
	   			}
	   			
	   			if (clusterWord.end > word.end) {
	   				word.end = clusterWord.end;
	   			}
	   			
	   			word.word += clusterWord.word;
	   			word.confidence += clusterWord.confidence;
	   		}
	   		
	   		word.confidence /= clusterWords.size();
	   		
	   		return word;
	   	}
    }
  
    

    public static List<SolrInputDocument> readMetadataFile(File metadataFile) throws IOException {
		System.out.println("(Meta) " + metadataFile.getName());
		
		Gson gson = new Gson();
		
		String json = FileUtils.readall(metadataFile);
		
		LinkedTreeMap deser = gson.fromJson(json, LinkedTreeMap.class);

		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", deser.get("filename") + "_progmeta");
		doc.addField("type", "progmeta");
		doc.addField("program", deser.get("filename"));
		doc.addField("synopsis", deser.get("description"));
		doc.addField("length", deser.get("duration"));
		doc.addField("title", deser.get("title"));
		
		docs.add(doc);
		
		return docs;
	}

	public static Map<Query, Set<Result>> importExpected(File queryFile,
														 File qRelFile) throws IOException, ParserConfigurationException, SAXException {
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
			
			Result result;
			
			if (parts.length == 4) {
				result = new Result(parts[1], Float.parseFloat(parts[2]),
						            Float.parseFloat(parts[3]), -1, -1);
			} else if (parts.length == 6) {
				// THIS IS WRONG!!! Also, comments.
				result = new Result(parts[1], Float.parseFloat(parts[2]),
			            					  Float.parseFloat(parts[3]),
			            					  Float.parseFloat(parts[4]),
			            					  Float.parseFloat(parts[5]));
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
}
