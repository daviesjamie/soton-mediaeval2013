/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.Image.Field;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.tracking.KLTHaarFaceTracker;
import org.openimaj.image.processing.face.tracking.clm.CLMFaceTracker;
import org.openimaj.image.processing.face.tracking.clm.MultiTracker.TrackedFace;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.processing.shotdetector.HistogramVideoShotDetector;
import org.openimaj.video.processing.shotdetector.VideoShotDetector;
import org.openimaj.video.timecode.VideoTimecode;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 *
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 16 Jul 2013
 */
public class VideoFaceTracker
{
	/**
	 * 	Encapsulates the information about when a face was seen. Note that the
	 * 	{@link Comparable} implementation checks for amount of overlap in the bounding
	 * 	boxes of the supplied face versus this face. If there's more than a given
	 * 	amount of overlap then the {@link #compareTo(FaceRange)} function returns 0
	 * 	(equality). It will return -1 for no overlap and +1 for some overlap but not enough.
	 * 	Note that this actually doesn't fit the contract of a {@link Comparator} where
	 * 	<code>sgn(x.compareTo(y)) == -sgn(y.compareTo(x))</code>, so it should be used carefully!
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 18 Jul 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	protected class FaceRange implements Comparable<DetectedFace>
	{
		public VideoTimecode start;
		public VideoTimecode end;
		public DetectedFace face;
		public FaceComparator comparator = new FaceComparator();
		public MBFImage bestFaceImage = null;
		public double bestFaceScore = 0;

		public FaceRange( final VideoTimecode start, final DetectedFace face, final double score )
		{
			this.start = start;
			this.face = face;
			this.bestFaceScore = score;
			this.bestFaceImage = MBFImage.createRGB( face.getFacePatch() );
		}

		@Override
		public int compareTo( final DetectedFace o )
		{
			return this.comparator.compare( this.face, o );
		}
	}

	/**
	 * 	For comparing two detected faces to see if they are approximately equal
	 * 	by using the boundary box overlap.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 18 Jul 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	public static class FaceComparator implements Comparator<DetectedFace>
	{
		public double overlapForSame = 0.8;

		@Override
		public int compare( final DetectedFace o1, final DetectedFace o2 )
		{
			// Calculate the number of pixels overlap between the two faces
			final Rectangle overlap = o1.getBounds().overlapping( o2.getBounds() );

			// If there's no overlap, return -1.
			if( overlap == null )
				return -1;

			final double pixelsOverlap = overlap.calculateArea();
			final double pcOverlap = Math.abs( pixelsOverlap / o1.getBounds().calculateArea() );

			// If the percentage overlap is greater than our threshold,
			// then this must be the same face, so we return it.
			if( pcOverlap > this.overlapForSame )
				return 0;

			return 1;
		}
	}

	/** The number of frames a face must continue to exist for to be considered a face */
	private final int minFrames = 30;

	/** The minimum size of a face detection in pixels */
	private final int minSize = 40;

	/** This is a counter for the number of frames detected faces exist for - only contains tracked faces */
	private final HashMap<FaceRange,Integer> frameCount = new HashMap<FaceRange, Integer>();

	/** The final list of faces we've found int he video */
	private final List<FaceRange> storedFaces = new ArrayList<FaceRange>();

	/** Whether we're debugging or not - whether to print/show stuff */
	public static final boolean DEBUG = true;

	/**
	 * 	Default constructor
	 */
	public VideoFaceTracker()
	{
	}

	public void processVideo( final File video )
	{
		// Create the video reader
		final XuggleVideo xv = new XuggleVideo( video );

		if( VideoFaceTracker.DEBUG )
			xv.seek( 20.5 );

		// Create a video shot detector
		final VideoShotDetector<MBFImage> sd = new HistogramVideoShotDetector( xv.getFPS() );

		// Create a face detector/tracker
		final KLTHaarFaceTracker kltTracker = new KLTHaarFaceTracker( this.minSize );
		kltTracker.setForceRetry( 10 );

		// A CLM Face tracker
		final CLMFaceTracker tracker2 = new CLMFaceTracker();
		tracker2.setRedetectEvery( 10 );

		// Read all the video frames
		VideoTimecode lastFrameTimecode = null;
		for( final MBFImage image : xv )
		{
			sd.processFrame( image );
			if( sd.wasLastFrameBoundary() )
				tracker2.reset();

			FImage frame = image.flattenMax();

			// Deinterlace the video
			frame = frame.getFieldInterpolate( Field.ODD );

			if( VideoFaceTracker.DEBUG )
				System.out.println( "Analysing frame "+xv.getCurrentTimecode() );

			// Look for faces to track with the KLT tracker
			final List<DetectedFace> faces1 = kltTracker.trackFace( frame );

			if( VideoFaceTracker.DEBUG )
				// DEBUG - Draw the KLT Tracked faces to the frame
				for( final DetectedFace f : faces1 )
					image.drawShape( f.getBounds(), RGBColour.CYAN );

			// Look for faces to track with the CLM tracker
			tracker2.track( frame );
			final List<TrackedFace> clmFaces = tracker2.getTrackedFaces();

			if( VideoFaceTracker.DEBUG )
				// DEBUG - Draw the CLM Tracked faces to the frame
				for( final TrackedFace f : clmFaces )
					CLMFaceTracker.drawFaceModel( image, f, true, true, true, true, true,
							tracker2.triangles, tracker2.connections, 1, RGBColour.YELLOW,
							RGBColour.WHITE, RGBColour.WHITE, RGBColour.RED );

			// Find agreements between the trackers
			final List<DetectedFace> faces = this.findTrackerAgreements( clmFaces, faces1 );

			// Check if we've got a face
			final List<FaceRange> toRemove = new ArrayList<FaceRange>();
			if( faces.size() > 0 )
			{
				if( VideoFaceTracker.DEBUG )
					System.out.println( "Found "+faces.size()+" faces.");

				// Map all the tracked faces to the existing faces.
				final List<FaceRange> f = new ArrayList<FaceRange>();
				FaceRange x = null;
				for( final DetectedFace face : faces )
					if( (x  = this.getExistingDetectedFace( face )) != null )
						f.add( x );

				// Now remove all those existing faces from our list of existing faces
				// so that we're left with the existing faces that we're no longer tracking.
				// These need to be removed.
				toRemove.addAll( this.frameCount.keySet() );
				toRemove.removeAll( f );

				// Loop through the detected faces
				for( final DetectedFace face : faces )
				{
					if( VideoFaceTracker.DEBUG )
						System.out.println( "Face "+face );

					if( VideoFaceTracker.DEBUG )
						System.out.println( "    - Confidence "+face.getConfidence() );

					if( face.getFacePatch() == null )
						face.setFacePatch( frame.extractROI( face.getBounds() ) );

					// Calculate the score for the face
					final double score = this.calculateScore( face, image );

					final FaceRange sameAs = this.getExistingDetectedFace( face );

					if( VideoFaceTracker.DEBUG )
						System.out.println( "        -> sameAs "+sameAs );

					// If we haven't found a similar detected face in the current face
					// list, then we'll add it in there.
					if( sameAs == null )
					{
						// Start a new frame counter for this face
						this.frameCount.put( new FaceRange( xv.getCurrentTimecode().clone(), face, score ), 1 );
					}
					else
					{
						// If we've found a similar detected face in the current face
						// list, we'll update it, rather than using the new one.
						this.frameCount.put( sameAs, this.frameCount.get( sameAs ) +1 );
						sameAs.face.setBounds( face.getBounds() );
						sameAs.face.setFacePatch( face.getFacePatch() );

						if( score > sameAs.bestFaceScore )
						{
							sameAs.bestFaceScore = score;
							sameAs.bestFaceImage = image.extractROI( face.getBounds() );
						}
					}

					if( VideoFaceTracker.DEBUG )
						// Draw the bounds onto the frame so we can see what's going on.
						image.drawShape( face.getBounds(), RGBColour.RED );
				}
			}
			else
			{
				// No current faces, so clear the frame counter
				toRemove.addAll( this.frameCount.keySet() );
			}

			// Check what faces we tracked and whether we should keep them
			for( final FaceRange face : toRemove )
			{
				// Check if the face was around for a long time.
				if( this.frameCount.get(face) > this.minFrames )
				{
					if( VideoFaceTracker.DEBUG )
						System.out.println( "Keeping "+face );

					this.storedFaces.add( face );
					face.end = lastFrameTimecode.clone();
				}
				// We'll ignore this face if it wasn't around long enough
				else
					if( VideoFaceTracker.DEBUG ) System.out.println( "Discarding "+face );

				// Remove this face from the tracked faces list.
				this.frameCount.remove( face );
			}

			if( VideoFaceTracker.DEBUG )
			{
				DisplayUtilities.displayName( image, "video" );
				this.displayPatches();
			}

			// Store the last frame timecode.
			lastFrameTimecode = xv.getCurrentTimecode();
		}
	}

	/**
	 * 	Attempts to find an existing detected face that fits the given face.
	 * 	This is worked out through the amount of overlap.
	 *
	 *	@param dt The face to find a match
	 *	@return The existing face (or null if none was found)
	 */
	private FaceRange getExistingDetectedFace( final DetectedFace dt )
	{
		for( final FaceRange existingFace : this.frameCount.keySet() )
		{
			if( existingFace.compareTo( dt ) == 0 )
				return existingFace;
		}

		return null;
	}

	/**
	 * 	Finds which faces between the results of two face detectors are the same by using
	 * 	bounding box overlap percentage. The returned list contains only those results which
	 * 	have matched between the two results, and it only contains the match from the first
	 * 	set of results (the second matching value is discarded).
	 *
	 *	@param faces1 The first set of face detection results
	 *	@param faces2 The second set of face detection results
	 *	@return An agreement set between the two
	 */
	private List<DetectedFace> findTrackerAgreements( final List<? extends DetectedFace> faces1,
			final List<? extends DetectedFace> faces2 )
	{
		// Relax the face comparator a little
		final FaceComparator fc = new FaceComparator();
		fc.overlapForSame = 0.7;

		final List<DetectedFace> df = new ArrayList<DetectedFace>();

		for( final DetectedFace face : faces1 )
			for( final DetectedFace face2 : faces2 )
				if( fc.compare( face, face2 ) == 0 )
					df.add( face );

		return df;
	}

	private double calculateScore( final DetectedFace face, final MBFImage frame )
	{
		final Rectangle r = face.getBounds();
		return r.calculateArea();
	}

	/**
	 * 	Display all the matching face patches
	 */
	private void displayPatches()
	{
		int w = 0;
		int h = 0;
		for( final FaceRange f : this.frameCount.keySet() )
		{
			w += f.face.getFacePatch().getWidth();
			h = Math.max( h, f.face.getFacePatch().getHeight() );
		}
		if( w == 0 ) w = 500;
		if( h == 0 ) h = 200;
		final FImage img = new FImage( w, h );
		int x = 0;
		for( final FaceRange f : this.frameCount.keySet() )
		{
			img.drawImage( f.face.getFacePatch(), x, 0 );
			img.drawImage( f.bestFaceImage.flatten(), x, f.face.getFacePatch().getHeight() );
			x += f.face.getFacePatch().getWidth();
		}
		DisplayUtilities.displayName( img, "Tracked Faces" ).pack();
	}

	/**
	 *	@param args
	 */
	public static void main( final String[] args )
	{
		if( args.length < 1 )
		{
			System.err.println( "ERROR: Please supply video file name." );
			System.exit(1);
		}

		// Get the file from the command-line
		final String videoFilename = args[0];
		final File videoFile = new File( videoFilename );

		final VideoFaceTracker vft = new VideoFaceTracker();
		vft.processVideo( videoFile );
	}
}
