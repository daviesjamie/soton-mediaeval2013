/**
 * 
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimaj.feature.FloatFV;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.FacialKeypoint;
import org.openimaj.image.processing.face.detection.keypoints.FacialKeypoint.FacialKeypointType;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.feature.FacePatchFeature;
import org.openimaj.image.processing.face.feature.FacePatchFeature.Extractor;
import org.openimaj.image.processing.face.recognition.FaceRecognitionEngine;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.iterator.TextLineIterable;

/**
 * Parkhi, et al. "On-the-fly Specific Person Retrieval"
 * 
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * @created 20 Aug 2013
 * @version $Author$, $Revision$, $Date$
 */
public class FaceTrackRecogniser
{
	/**
	 * Class for storing a loaded face track.
	 * 
	 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
	 * @created 20 Aug 2013
	 * @version $Author$, $Revision$, $Date$
	 */
	public static class FaceTrack
	{
		/**
		 * A single instance of a bounds in a face track
		 * 
		 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
		 * @created 20 Aug 2013
		 * @version $Author$, $Revision$, $Date$
		 */
		public static class FaceTrackBounds
		{
			/** The frame number */
			public int frame;

			/** The bounding box */
			public Rectangle bounds;

			/** The score */
			public float score;
		}

		/** The list of face track objects */
		public List<FaceTrackBounds> track = new ArrayList<FaceTrackBounds>();

		/**
		 * Loads a face track file that's been output by the VideoFaceTracker
		 * 
		 * @param f The file
		 * @return a new FaceTrack
		 */
		public static FaceTrack load( File f )
		{
			FaceTrack ft = new FaceTrack();
			
			// Pattern the match the bounds in the text line
			Pattern frameAndScorePattern = Pattern.compile( "^([0-9]+)\\s:\\s([0-9.]+)");
			Pattern rectPattern = Pattern.compile( 
				"Rectangle\\[x=([0-9.]+),y=([0-9.]+),width=([0-9.]+),height=([0-9.]+)\\]");
			
			for( String line : new TextLineIterable( f ) )
			{
				// Create the matchers for the patterns
				Matcher m1 = rectPattern.matcher( line );
				Matcher m2 = frameAndScorePattern.matcher( line );
				
				// This is what we'll add into the list if we find everything
				FaceTrackBounds ftb = new FaceTrackBounds();

				// Check we've got a rectangle
				if( m1.find() )
				{
					// Check we've got a frame number
					if( m2.find() )
					{
						// Parse all the values from the regex match
						ftb.frame = Integer.parseInt( m2.group(1) );
						ftb.score = Float.parseFloat( m2.group(2) );
						ftb.bounds = new Rectangle(
								Float.parseFloat( m1.group(1) ),
								Float.parseFloat( m1.group(2) ),
								Float.parseFloat( m1.group(3) ),
								Float.parseFloat( m1.group(4) )
						);
						
						// Add this bounds object into the list
						ft.track.add( ftb );
					}
				}
			}
			
			return ft;
		}
	}
	
	public void matchPerson( MBFImage img )
	{
		
	}

	public static void main( String[] args ) throws IOException
	{
		String base = "/home/dd/Dropbox/Work/mediaeval/faces/cache/"
				+ "20080401_010000_bbcfour_the_book_quiz.webm/000627-001071";
		
//		FaceTrack ft = FaceTrack.load( new File( base+"-tracks.txt" ) );
//		System.out.println( ft );
		
		MBFImage repImg = ImageUtilities.readMBF( new File(base+"-face.png") );
		DisplayUtilities.display( repImg );		
		
		FKEFaceDetector fkd = new FKEFaceDetector();
		List<KEDetectedFace> faces = fkd.detectFaces( repImg.flatten() );
		
		// There should only be one.
		if( faces.size() > 0 )
		{
			KEDetectedFace face = faces.get( 0 );
			
			Extractor fpfe = new FacePatchFeature.Extractor();
			FacePatchFeature fpf = fpfe.extractFeature( face );
			
			FloatFV fv = fpf.getFeatureVector();
			System.out.println( fv.values.length );
		}
	}
}
