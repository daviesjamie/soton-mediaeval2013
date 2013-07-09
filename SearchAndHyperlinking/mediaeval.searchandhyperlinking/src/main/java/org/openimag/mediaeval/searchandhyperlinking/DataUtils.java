package org.openimag.mediaeval.searchandhyperlinking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.math.geometry.shape.Circle;

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
}
