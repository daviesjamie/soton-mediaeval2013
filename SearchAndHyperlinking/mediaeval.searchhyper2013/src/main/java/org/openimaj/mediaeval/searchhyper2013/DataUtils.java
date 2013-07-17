package org.openimaj.mediaeval.searchhyper2013;

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


public abstract class DataUtils {
	public static final String[] badWords = { "bbc" };
	
	public static final int X_DIM = 800;
	
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

	public static void cleanQuery(Query q) {
		String queryText = q.getQueryText();
	
		for (String noise : badWords) {
			Pattern p = Pattern.compile(noise, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(queryText);
			
			queryText = m.replaceAll("");
		}
		
		q.setQueryText(queryText);
	}

	public static float StoMS(float startTime) {
		int mins = (int) Math.floor(startTime / 60);
		int secs = (int) (((startTime / 60) - mins) * 60);
		
		return mins + (secs / 100f);
	}
	
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

	public static int clusterHierarchyDepth(ClusterHierarchyNode root, int acc) {
		if (root.getChildren() == null) {
			return acc;
		}
		
		return Math.max(clusterHierarchyDepth((ClusterHierarchyNode) root.getChildren().get(0), acc + 1),
						clusterHierarchyDepth((ClusterHierarchyNode) root.getChildren().get(1), acc + 1));
	}
	
	public static int clusterHierarchyDepth(ClusterHierarchyNode root) {
		return clusterHierarchyDepth(root, 1);
	}
	
	public static MBFImage visualiseDataClean(ClusterHierarchyNode<Result, DefaultCluster<Result>> root, float programLength) {
		int maxDepth = clusterHierarchyDepth(root);
		
		int HEIGHT = 50;
		int Y_DIM = HEIGHT * maxDepth;
		
		MBFImage vis = new MBFImage(X_DIM, Y_DIM, ColourSpace.HSV);
		
		int depth = 0;
		
		List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> nextLevel = 
			new ArrayList<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
		nextLevel.add(root);
		
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

	public static MBFImage visualiseData(ClusterHierarchyNode<Result, DefaultCluster<Result>> root, float programLength) {
		int maxDepth = clusterHierarchyDepth(root);

		int WIDTH = 50;
		int Y_DIM = WIDTH * (maxDepth + 1);
		
		float hueStep = 1f / maxDepth;
		
		MBFImage vis = new MBFImage(X_DIM, Y_DIM, ColourSpace.HSV);
		
		int depth = 0;
		
		List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> nextLevel = 
			new ArrayList<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
		nextLevel.add(root);
		
		while (!nextLevel.isEmpty()) {
			List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> newNextLevel =
				new ArrayList<ClusterHierarchyNode<Result, DefaultCluster<Result>>>();
			
			for (ClusterHierarchyNode<Result, DefaultCluster<Result>> node : nextLevel) {
				Result result = clusterToResult(node);
				
				int x0 = (int)((result.getStartTime() / programLength) * X_DIM);
				int x1 = (int)((result.getEndTime() / programLength) * X_DIM);
				int y = WIDTH * (depth + 1);
				Float[] col = { depth*hueStep, 1f, 1f };

				Rectangle rect = new Rectangle(x0, y, x1 - x0 + 1, WIDTH - 5);
				vis.drawShapeFilled(rect, col);
				
				if (node.getChildren() != null) {
					newNextLevel.addAll(node.getChildren());
				} else if (node.getMembers() != null) {
					System.out.println(node.getMembers());
					for (Result member : node.getMembers()) {
						x0 = (int)((member.getStartTime() / programLength) * X_DIM);
						x1 = (int)((member.getEndTime() / programLength) * X_DIM);
						y = WIDTH * (depth + 2);
						Float[] col2 = { (depth + 1)*hueStep, 1f, 1f };

						rect = new Rectangle(x0, y, x1 - x0 + 1, WIDTH - 5);
						vis.drawShapeFilled(rect, col2);
					}
				}
			}
			
			nextLevel = newNextLevel;
			
			depth++;
		}
		
		return Transforms.HSV_TO_RGB(vis);
	}

	public static MBFImage visualiseData(
			List<ClusterHierarchyNode<Result, DefaultCluster<Result>>> children,
			int height, Float progLength, Float[] col) {
		List<Result> results = new ArrayList<Result>();
		
		for (ClusterHierarchyNode<Result, DefaultCluster<Result>> node : children) {
			results.add(clusterToResult(node));
		}
		
		return visualiseData(results, height, progLength, col);
	}

	public static MBFImage visualiseData(Result clusterToResult, int height,
			Float progLength, Float[] green) {
		List<Result> list = new ArrayList<Result>();
		list.add(clusterToResult);
		
		return visualiseData(list, height, progLength, green);
	}

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
			
		})/*.sort(new Comparator<Row>() {

			@Override
			public int compare(Row arg0, Row arg1) {
				return ((Integer) arg0.get("count")).compareTo((Integer) arg1.get("count"));
			}
			
		})*/;
		
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
			
			//Float score = (((Float) row.get("score")));
			//Float score = (maxScore) / (Integer) row.get("count");
			Float score = (Float) row.get("score") / (Integer) row.get("count");
			
			query += "\"" + row.get("word") + "\"^" + score + " ";
		}
		
		return query;
	}

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
}
