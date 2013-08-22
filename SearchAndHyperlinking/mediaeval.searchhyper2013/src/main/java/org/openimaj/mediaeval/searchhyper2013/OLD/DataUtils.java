package org.openimaj.mediaeval.searchhyper2013.OLD;

import gov.sandia.cognition.learning.algorithm.clustering.cluster.Cluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.NearestNeighboursFactory;
import org.openimaj.math.geometry.shape.Circle;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.util.function.Predicate;

import com.github.wcerfgba.Row;
import com.github.wcerfgba.Table;

/**
 * Utility methods for working with data, mainly for visualising.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public abstract class DataUtils {
	// X dimension for images.
	public static final int X_DIM = 800;

	/**
	 * Takes in an array of segments (start and end times as doubles) and plots 
	 * them on a timeline as circles.
	 * 
	 * @param array   The data array to plot.
	 * @param maxVal  The maximum value for data to have as a time component
	 * 				  (typically this is the program length).
	 * @param col	  Colour.
	 * @return		  MBFImage plot.
	 */
	public static MBFImage visualiseData(double[][] array, double maxVal, Float[] col) {
		int Y_DIM = 200;
		
		MBFImage img = new MBFImage(X_DIM, Y_DIM, ColourSpace.RGB);
		
		for (double[] clip : array) {				
			float x = (float) (((clip[0] + clip[1]) / 2) * (X_DIM / maxVal));
			float y = Y_DIM / 2;
			float radius = (float) ((clip[1] - clip[0]) * (X_DIM / maxVal));
			
			Circle circ = new Circle(x, y, radius);
			
			img.drawShape(circ, col);
		}
		
		return img;
	}

	/**
	 * Plots a List<Result> as start and end lines going vertically on an image 
	 * with colour scaled by confidence score.
	 * 
	 * @param results 	  Array to plot.
	 * @param height	  Height of image to create.
	 * @param progLength  Max. length of program (for calculating X values).
	 * @param col		  Base colour.
	 * @return			  MBFImage plot of the data.
	 */
	public static MBFImage visualiseData(List<Result> results, int height, float progLength, Float[] col) {
		
		float maxConf = 0;
		
		for (Result result : results) {
			if (result.getConfidenceScore() > maxConf) {
				maxConf = result.getConfidenceScore();
			}
		}
		
		MBFImage vis = new MBFImage(X_DIM, height, ColourSpace.RGB);
		
		for (int i = 0; i < results.size(); i++) {
			Result result = results.get(i);
			
			int xStart = (int)((result.getStartTime() / progLength) * X_DIM);
			int xEnd = (int)((result.getEndTime() / progLength) * X_DIM);
			
			Float[] colour = new Float[3];
			
			for (int j = 0; j < colour.length; j++) {
				colour[j] = (result.getConfidenceScore() / maxConf) * col[j];
			}
			
			vis.drawLine(xStart, 0, xStart, height, 1, colour);
			vis.drawLine(xEnd, 0, xEnd, height, 1, colour);
		}
		
		return vis;
	}
	
	/**
	 * Converts a cluster hierarchy into a List<Result>.
	 * 
	 * @param resultClusterHierarchy
	 * @return
	 */
	public static List<Result> flattenClusterHierarchy(ClusterHierarchyNode<Result, Cluster<Result>> resultClusterHierarchy) {
		Set<Result> resultSet = new HashSet<Result>();
		
		Queue<ClusterHierarchyNode<Result, Cluster<Result>>> nodeQueue =
				new ConcurrentLinkedQueue<ClusterHierarchyNode<Result, Cluster<Result>>>();
			nodeQueue.add(resultClusterHierarchy);
		
		while(!nodeQueue.isEmpty()) {
			ClusterHierarchyNode<Result, Cluster<Result>> cur = 
				nodeQueue.poll();
			
			resultSet.add(clusterToResult(cur));
			
			if (cur.getChildren() != null) {
				nodeQueue.addAll(cur.getChildren());
			}
		}
		
		List<Result> resultList = new ArrayList<Result>(resultSet);
		
		Collections.sort(resultList, Collections.reverseOrder(new Result.ResultLengthComparator()));
		
		return resultList;
	}

	/**
	 * Converts a float representing seconds into a float representing 
	 * minutes.seconds.
	 * 
	 * @param startTime
	 * @return
	 */
	public static float StoMS(float startTime) {
		int mins = (int) Math.floor(startTime / 60);
		int secs = (int) (((startTime / 60) - mins) * 60);
		
		return mins + (secs / 100f);
	}

	/**
	 * Clusters results using DBSCAN.
	 * 
	 * @param results	  Input results.
	 * @param eps		  Epsilon for DBSCAN.
	 * @param minSize	  Minimum cluster size for DBSCAN.
	 * @param visualise	  Visualise the clustering along the way?
	 * @param progLength  Maximum program length (for visualising).
	 * @return			  A new List<Result> representing the clusters.
	 */
	public static List<Result> clusterResults(List<Result> results,
											  float eps,
											  int minSize,
											  boolean visualise,
											  float progLength) {
		List<Result> progResults = new ArrayList<Result>();
		if (results.isEmpty()) return progResults;
		
		String program = results.get(0).getProgram();
		
		double[][] data = new double[results.size()][2];
		for (int i = 0; i < results.size(); i++) {
			data[i][0] = results.get(i).getStartTime();
			data[i][1] = results.get(i).getEndTime();
		}
		
		MBFImage vis = null;
		if (visualise) vis = DataUtils.visualiseData(data, progLength, RGBColour.GREEN);
		
		NearestNeighboursFactory<DoubleNearestNeighboursExact, double[]> nnFactory =
			new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.EUCLIDEAN);
		DBSCANConfiguration<DoubleNearestNeighbours, double[]> dbscanConfig = 
			new DBSCANConfiguration<DoubleNearestNeighbours, double[]>(2, eps, minSize, nnFactory);
		DoubleDBSCAN dbscan = new DoubleDBSCAN(dbscanConfig);
		
		DoubleDBSCANClusters clusters = dbscan.cluster(data);
		
		float maxConf = 0;
		
		boolean clustered = false;
		
		int[][] indices = clusters.getClusterMembers();
		for (int i = 0; i < indices.length; i++) {
			double min = Double.MAX_VALUE;
			double max = 0;
			
			// Calculate cluster bounds from members.
			for (int j = 0; j < indices[i].length; j++) {
				double start = data[indices[i][j]][0];
				double end = data[indices[i][j]][1];
				
				if (start < min) min = start;
				if (end > max) max = end;
			}
			
			double[][] minMaxData = { { min, max } };
			
			if (visualise) vis.addInplace(
								DataUtils.visualiseData(minMaxData,
														progLength,
														RGBColour.RED));
			clustered = true;
			
			if (maxConf < indices[i].length) maxConf = indices[i].length;
			
			Result clusterResult =
				new Result(program, (float) min, (float) max, (float) min, indices[i].length);
			progResults.add(clusterResult);
		}
		
		// Recalculate relevance.
		for (int i = 0; i < indices.length; i++) {
			float avgElementRelevance = 0;
			
			for (int j = 0; j < indices[i].length; j++) {
				Result in = results.get(j);
				avgElementRelevance += in.getConfidenceScore();
			}
			avgElementRelevance /= indices[i].length;
			
			Result out = progResults.get(i);
			
			float confidenceScore = avgElementRelevance * 
									(out.getConfidenceScore() / maxConf);
			out.setConfidenceScore(confidenceScore);
		}
		
		if (visualise && clustered)
			DisplayUtilities.displayName(vis, program);
		
		return progResults;
	}
	
	public static Result clusterToResult(Cluster<Result> cluster) {
		return resultCollectionToResult(cluster.getMembers());
	}

	/**
	 * Returns the maximum depth of a cluster hierarchy.
	 * 
	 * @param root  Root node.
	 * @param acc   Accumulator value.
	 * @return		Depth.
	 */
	public static int clusterHierarchyDepth(ClusterHierarchyNode root, int acc) {
		if (root.getChildren() == null) {
			return acc;
		}
		
		return Math.max(clusterHierarchyDepth((ClusterHierarchyNode) root.getChildren().get(0), acc + 1),
						clusterHierarchyDepth((ClusterHierarchyNode) root.getChildren().get(1), acc + 1));
	}
	
	/**
	 * Returns the maximum depth of a cluster hierarchy.
	 * 
	 * @param root  Root node.
	 * @return		Depth.
	 */
	public static int clusterHierarchyDepth(ClusterHierarchyNode root) {
		return clusterHierarchyDepth(root, 1);
	}

	/**
	 * Visualises a cluster hierarchy on multiple levels, changing the hue as 
	 * we descend the hierarchy.
	 * 
	 * @param root			 Root node.
	 * @param programLength  Program length for calculating X values.
	 * @return				 MBFImage of the visualisation.
	 */
	public static MBFImage visualiseData(ClusterHierarchyNode<Result, DefaultCluster<Result>> root, float programLength) {
		int maxDepth = clusterHierarchyDepth(root);
		
		int HEIGHT = 50;
		int Y_DIM = HEIGHT * maxDepth;
		
		MBFImage vis = new MBFImage(X_DIM, Y_DIM, ColourSpace.HSV);
		
		int depth = 0;
		
		List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> nextLevel = 
			new ArrayList<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
		nextLevel.add(root);
	
		// We add child nodes for the current level to the nextLevel list so 
		// that we can process the tree in a breadth-first manner.
		while (!nextLevel.isEmpty()) {
			List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> newNextLevel =
				new ArrayList<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
			
			for (ClusterHierarchyNode<Result, DefaultCluster<Result>> node : nextLevel) {
				List<Result> thisLevel = new ArrayList<Result>(node.getMembers());
				
				if (node.getChildren() != null) {
					for (ClusterHierarchyNode<Result, DefaultCluster<Result>> child : node.getChildren()) {
						newNextLevel.add(child);
					}
				}
				
				Result clusterResult = resultCollectionToResult(thisLevel);
				
				int x0 = (int)((clusterResult.getStartTime() / programLength) * X_DIM);
				int x1 = (int)((clusterResult.getEndTime() / programLength) * X_DIM);
				int y = HEIGHT * depth;
				Float[] col = { Math.min(depth * (1f / maxDepth), 1f), 1f, 1f };

				Rectangle rect = new Rectangle(x0, y, x1 - x0, HEIGHT - 2);
				vis.drawShapeFilled(rect, col);
			}
			
			nextLevel = newNextLevel;
			depth++;
		}
		
		return Transforms.HSV_TO_RGB(vis);
	}
	
	/**
	 * Aggregates a Collection<Result> into a result by discovering the minimum 
	 * start time, maximum end time, and average confidence.
	 * 
	 * @param thisLevel
	 * @return
	 */
	private static Result resultCollectionToResult(Collection<Result> thisLevel) {
		String program = null;
		float start = Float.MAX_VALUE;
		float end = 0;
		float totalConf = 0;
		
		float minConf = Float.MAX_VALUE;
		float maxConf = 0;
		
		Result[] res = thisLevel.toArray(new Result[0]);

		for (int i = 0; i < res.length; i++) {
			program = res[i].getProgram();
			
			if (res[i].getStartTime() < start) {
				start = res[i].getStartTime();
			}
			
			if (res[i].getEndTime() > end) {
				end = res[i].getEndTime();
			}

			totalConf += res[i].getConfidenceScore();
			
			if (res[i].getConfidenceScore() < minConf) {
				minConf = res[i].getConfidenceScore();
			}
			
			if (res[i].getConfidenceScore() > maxConf) {
				maxConf = res[i].getConfidenceScore();
			}
		}
		
		return new Result(program, start, end, start, totalConf / res.length);
	}

	/**
	 * Converts a List<CLusterHierarchyNode> to a List<Result> and visualises it
	 * with {@link visualiseData(List<Result> results, int height, float progLength, Float[] col) visualiseData()}.
	 *  
	 * @param children
	 * @param height
	 * @param progLength
	 * @param col
	 * @return
	 */
	public static MBFImage visualiseData(
			List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> children,
			int height, Float progLength, Float[] col) {
		List<Result> results = new ArrayList<Result>();
		
		for (ClusterHierarchyNode<Result, DefaultCluster<Result>> node : children) {
			results.add(clusterToResult(node));
		}
		
		return visualiseData(results, height, progLength, col);
	}

	/**
	 * Visualises a single Result with
	 * {@link #visualiseData(List<Result> results, int height, float progLength, Float[] col) visualiseData()}.
	 * 
	 * @param clusterToResult
	 * @param height
	 * @param progLength
	 * @param green
	 * @return
	 */
	public static MBFImage visualiseData(Result clusterToResult, int height,
			Float progLength, Float[] green) {
		List<Result> list = new ArrayList<Result>();
		list.add(clusterToResult);
		
		return visualiseData(list, height, progLength, green);
	}

	/**
	 * Creates an expansion query from a list of SolrDocuments by counting the 
	 * most popular words and using words with the highest scores.
	 *  
	 * @param docs
	 * @return      Query string.
	 */
	public static String createExpansionQuery(SolrDocumentList docs) {
		Table wordTable = new Table("word", "count", "score");
		
		for (SolrDocument doc : docs) {
			String[] words = ((String) doc.getFieldValue("phrase")).split("\\s");
			
			for (String word : words) {
				String trimmedWord = word.replaceAll("^\\W+|\\W+$", "").trim().toLowerCase();
				
				if (trimmedWord.equals("")) continue;
				
				Row wordRow = wordTable.matchingRow(trimmedWord, null, null);
				
				if (wordRow != null) {
					wordRow.set("count", ((Integer) wordRow.get("count")) + 1);
					wordRow.set("score", ((Float) wordRow.get("score")) +  
										 ((Float) doc.getFieldValue("score")));
				} else {
					wordTable.addRow(trimmedWord, 1, doc.getFieldValue("score"));
				}
			}
		}
		
		wordTable = wordTable.sort(new Comparator<Row>() {

			@Override
			public int compare(Row arg0, Row arg1) {
				return ((Float) arg1.get("score")).compareTo((Float) arg0.get("score"));
			}
			
		});
		
		int maxCount = 0;
		
		for (Row row : wordTable) {
			if ((Integer) row.get("count") > maxCount) {
				maxCount = (Integer) row.get("count");
			}
		}
		
		float maxScore = 0;
		
		for (Row row : wordTable) {
			if ((Float) row.get("score") > maxScore) {
				maxScore = (Float) row.get("score");
			}
		}
		
		String query = "";
		
		for (int i = 0; i < 100 && i < wordTable.size(); i++) {
			Row row = wordTable.getRow(i);
			
			Float score = (Float) row.get("score") / (Integer) row.get("count");
			
			query += "\"" + row.get("word") + "\"^" + score + " ";
		}
		
		return query;
	}

	/**
	 * Converts SolrDocuments to Results and visualises them with 
	 * {@link #visualiseData(List<Result> results, int height, float progLength, Float[] col) visualiseData()}.
	 * 
	 * @param solrDocs
	 * @param height
	 * @param progLength
	 * @param col
	 * @return
	 */
	public static MBFImage visualiseData(
			SolrDocumentList solrDocs, int height,
			Float progLength, Float[] col) {
		List<Result> results = new ArrayList<Result>();
		
		for (SolrDocument doc : solrDocs) {
			results.add(
				new Result(
					(String) doc.getFieldValue("program"),
					(Float) doc.getFieldValue("start"),
					(Float) doc.getFieldValue("end"),
					(Float) doc.getFieldValue("start"),
					(Float) doc.getFieldValue("score")));
		}
		
		return visualiseData(results, height, progLength, col);
	}

	public static float HMStoS(String hmsString) {
		String[] parts = hmsString.split(":");
		
		float secs = Float.parseFloat(parts[2]);
		secs += Float.parseFloat(parts[1]) * 60;
		secs += Float.parseFloat(parts[0]) * 60 * 60;
		
		return secs;
	}
}
