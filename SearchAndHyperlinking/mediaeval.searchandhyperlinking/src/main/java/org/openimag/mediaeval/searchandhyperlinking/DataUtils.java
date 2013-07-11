package org.openimag.mediaeval.searchandhyperlinking;

import gov.sandia.cognition.learning.algorithm.clustering.cluster.Cluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.NearestNeighboursFactory;
import org.openimaj.math.geometry.shape.Circle;
import org.openimaj.ml.clustering.dbscan.DBSCANConfiguration;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;

public abstract class DataUtils {
	public static final String[] badWords = { "bbc" };
	
	public static MBFImage visualiseData(double[][] array, double maxVal, Float[] col) {
		int X_DIM = 800;
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
	
	public static class ResultTimeComparator implements Comparator<Result> {

		@Override
		public int compare(Result o1, Result o2) {
			float diff = o1.getStartTime() - o2.getStartTime();
			
			if (diff < 0) {
				return -1;
			} else if (diff == 0) {
				return 0;
			} else {
				return 1;
			}
		}
		
	}
	
	public static class ResultConfidenceComparator implements Comparator<Result> {

		@Override
		public int compare(Result o1, Result o2) {
			float diff = o2.getConfidenceScore() - o1.getConfidenceScore();
			
			if (diff < 0) {
				return -1;
			} else if (diff == 0) {
				return 0;
			} else {
				return 1;
			}
		}
		
	}
	
	public static class ResultProgramComparator implements Comparator<Result> {

		@Override
		public int compare(Result o1, Result o2) {
			return o2.getProgram().compareTo(o1.getProgram());
		}
		
	}
	
	public static Result clusterToResult(Cluster<Result> cluster) {
		String program = null;
		float start = Float.MAX_VALUE;
		float end = 0;
		float confidence = 0;
		float avgDistance = 0;
		
		Result[] res = cluster.getMembers().toArray(new Result[0]);
		for (int i = 0; i < res.length; i++) {

			for (int j = i; j < res.length; j++) {
				avgDistance += res[i].distanceTo(res[j]);
			}
			
			program = res[i].getProgram();
			
			if (res[i].getStartTime() < start)
				start = res[i].getStartTime();
			
			if (res[i].getEndTime() > end)
				end = res[i].getEndTime();
			
			confidence += res[i].getConfidenceScore();
		}
		
		avgDistance /= (res.length * (res.length - 1)) / 2;
		confidence /= cluster.getMembers().size();
		
		confidence *= avgDistance / (end - start);
		
		return new Result(program, start, end, start, confidence);
	}
}
