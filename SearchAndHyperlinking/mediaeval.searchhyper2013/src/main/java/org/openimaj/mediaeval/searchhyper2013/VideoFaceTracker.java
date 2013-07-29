/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.Image.Field;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.detection.CLMDetectedFace;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.tracking.KLTHaarFaceTracker;
import org.openimaj.image.processing.face.tracking.clm.CLMFaceTracker;
import org.openimaj.image.processing.face.tracking.clm.MultiTracker.TrackedFace;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.math.geometry.shape.Shape;
import org.openimaj.video.processing.shotdetector.LocalHistogramVideoShotDetector;
import org.openimaj.video.processing.shotdetector.ShotBoundary;
import org.openimaj.video.processing.shotdetector.ShotDetectedListener;
import org.openimaj.video.processing.shotdetector.VideoKeyframe;
import org.openimaj.video.processing.shotdetector.VideoShotDetector;
import org.openimaj.video.timecode.VideoTimecode;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 *	This is a class that tracks faces forwards though a video. It uses the
 *	agreement between a number of face trackers to attempt to limit false positives
 *	and uses a heuristic (see {@link FaceComparator}) to determine the "best"
 *	face within the track. This best face image is stored alongside the start
 *	and end timecodes of the visibility of the face (in a {@link FaceRange}).
 *	<p>
 *	Faces for which the range is less than a configured number of frames are
 *	removed from the results set. The default number of frames is 30.
 *	<p>
 *	If the video does not have square pixels (e.g. it's anamorphic), you may
 *	need to set the pixel aspect ratio with {@link #setAspect(double)}. A typical
 *	setting for widescreen anamorphic DVB video would be 1.7777777 (16/9). Correcting
 *	the video aspect will help the face detectors to find faces while also ensuring
 *	the detected faces are of the correct aspect.
 *	<p>
 *	If your video is interlaced and has fast moving content you may wish to
 *	deinterlace the video during the processing. You can deinterlace the video
 *	using {@link #setDeinterlaceVideo(boolean)} with <code>true</code>. The default
 *	is false.
 *	<p>
 *	If you intend on analysing long videos, you may need to limit the amount
 *	of memory consumed by the process. To do this you can avoid storing the
 *	best face images into the face range objects by using {@link #setCacheImages(boolean)}
 *	with <code>false</code>. You will then need to write images to disk using
 *	{@link #setCacheDir(String)} with the location of the cache directory. If
 *	no cache directory is configured, the images will be stored in the {@link FaceRange}
 *	objects regardless of the value of {@link #isCacheImages()}. The default is
 *	to cache faces in a directory called <code>cache</code> in the current
 *	directory. The default is also to store face images to memory.
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
		public double overlapForSame = 0.6;

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

	/**
	 * 	A class for storing information to output to the track file.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 25 Jul 2013
	 */
	public static class TrackInfo
	{
		public Shape bounds;
		public double score;

		public TrackInfo( final Shape s, final double sc )
		{
			this.bounds = s;
			this.score = sc;
		}
	}

	/** Whether we're debugging or not - whether to print/show stuff */
	public static final boolean DEBUG = true;

	/** Whether we're drawing stuff to the image */
	public static final boolean IMAGE_DEBUG = VideoFaceTracker.DEBUG && false;

	/** If cacheImages == true, this is where we'll store them */
	public String cacheDir = "cache/";

	/** Whether images are being cached to disk */
	public boolean cacheImages = true;

	/** Whether face images will be stored to memory */
	public boolean storeFaceImages = true;

	/** Whether to deinterlace the video frames */
	public boolean deinterlaceVideo = false;

	/** Whether to cache shot boundaries */
	public boolean cacheShotBoundaries = true;

	/** Whether to cache the face tracks in ASCII files */
	public boolean cacheFaceTracks = true;

	/** The number of frames a face must continue to exist for to be considered a face */
	private final int minFrames = 30;

	/** The minimum size of a face detection in pixels */
	private final int minSize = 80;

	/** This is a counter for the number of frames detected faces exist for - only contains tracked faces */
	private final HashMap<FaceRange,Integer> frameCount = new HashMap<FaceRange, Integer>();

	/** The final list of faces we've found in the video */
	private final List<FaceRange> storedFaces = new ArrayList<FaceRange>();

	/** The aspect ratio of the pixels (1 = no change) */
	public double aspect = 1;

	/** The amount to enlarge bounds before saving */
	private final float enlargeBoundsAmount = 2f;

	/** The number of times to downscale the video (2 = half size) */
	private double downscale = 2;

	/** The number of frames between each image process (1 = every frame) */
	private int nSkipFrames = 1;

	/** The amount by which a frame must change to be a shot boundary */
	private double shotBoundaryThresholdRatio = 0.06;

	/**
	 * 	Default constructor
	 */
	public VideoFaceTracker()
	{
	}

	/**
	 * 	Returns the list of faces that were stored during the processing.
	 *	@return The list of stored faces
	 */
	public List<FaceRange> getStoredFaces()
	{
		return this.storedFaces;
	}

	/**
	 * 	Process the given video
	 *	@param video The video to process.
	 */
	public void processVideo( final File video )
	{
		// Create the video reader
		final XuggleVideo xv = new XuggleVideo( video );

		if( VideoFaceTracker.DEBUG )
			xv.seek( 20.5 );

		// Create a video shot detector
		final VideoShotDetector<MBFImage> sd = new LocalHistogramVideoShotDetector( xv, 4 );
		sd.setFindKeyframes( false );	// Don't store key frames in the shot detector
		sd.setThreshold( this.shotBoundaryThresholdRatio );
		if( this.cacheShotBoundaries )
		{
			sd.addShotDetectedListener( new ShotDetectedListener<MBFImage>()
			{
				@Override
				public void shotDetected( final ShotBoundary<MBFImage> sb, final VideoKeyframe<MBFImage> vk )
				{
					VideoFaceTracker.this.cacheShotBoundary( video, xv.getCurrentFrameIndex(), vk );
				}

				@Override
				public void differentialCalculated( final VideoTimecode vt, final double d, final MBFImage frame ) {}
			} );
		}

		// Create a face detector/tracker
		final KLTHaarFaceTracker kltTracker = new KLTHaarFaceTracker( this.minSize );
		kltTracker.setForceRetry( 10 );

		// A CLM Face tracker
		final CLMFaceTracker tracker2 = new CLMFaceTracker();
		tracker2.getModelTracker().getInitialVars().faceDetector.set_min_size( this.minSize );
		tracker2.setRedetectEvery( 10 );

		VideoTimecode lastFrameTimecode = null;

		// We'll store all the tracks for the current faces here and cache them later.
		final HashMap<FaceRange,TreeMap<Integer,TrackInfo>> currentTracks
			= new HashMap<FaceRange, TreeMap<Integer,TrackInfo>>();

		// Read all the video frames
		for( MBFImage image : xv )
		{
			// Frame skipping
			if( xv.getCurrentFrameIndex() % this.nSkipFrames != 0 )
				continue;

			// Process with the shot detector. This is necessary to know whether
			// to reset the CLM tracker.
			sd.processFrame( image );
			if( sd.wasLastFrameBoundary() )
				tracker2.reset();

			// If the aspect ratio is anything other than 1, we'll resample
			// the frame to fix it's shape.
			if( this.aspect != 1 )
			{
				image = image.process( new ResizeProcessor(
					(int) (image.getHeight()*this.aspect/this.downscale),
					(int) (image.getHeight()/this.downscale), false  ) );
			}

			// The face trackers work on greyscale images, so we'll flatten the colour image.
			FImage frame = image.flattenMax();

			// Deinterlace the video
			if( this.deinterlaceVideo )
				frame = frame.getFieldInterpolate( Field.ODD );

			if( VideoFaceTracker.DEBUG )
				System.out.println( "Analysing frame "+xv.getCurrentTimecode() );

			// Look for faces to track with the KLT tracker
			final List<DetectedFace> faces1 = kltTracker.trackFace( frame );

			if( faces1.size() == 0 )
				continue;

			if( VideoFaceTracker.IMAGE_DEBUG )
				// DEBUG - Draw the KLT Tracked faces to the frame
				for( final DetectedFace f : faces1 )
					image.drawShape( f.getBounds(), RGBColour.CYAN );

			// Look for faces to track with the CLM tracker
			tracker2.track( frame );
			final List<TrackedFace> clmFaces = tracker2.getTrackedFaces();

			if( VideoFaceTracker.IMAGE_DEBUG )
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
					if( VideoFaceTracker.DEBUG )
						System.out.println( "        -> Score: "+score );

					// If we haven't found a similar detected face in the current face
					// list, then we'll add it in there.
					if( sameAs == null )
					{
						// Start a new frame counter for this face
						final FaceRange fr = new FaceRange( xv.getCurrentTimecode().clone(), face, score );
						final Rectangle enlargedBounds = face.getBounds().clone();
						enlargedBounds.scaleCOG( this.enlargeBoundsAmount );
						fr.bestFaceImage = image.extractROI( enlargedBounds );
						this.frameCount.put( fr, 1 );
					}
					else
					{
						// If we've found a similar detected face in the current face
						// list, we'll update it, rather than using the new one.
						this.frameCount.put( sameAs, this.frameCount.get( sameAs ) + this.nSkipFrames );
						sameAs.face.setBounds( face.getBounds() );
						sameAs.face.setFacePatch( face.getFacePatch() );

						if( score > sameAs.bestFaceScore )
						{
							sameAs.bestFaceScore = score;
							final Rectangle enlargedBounds = face.getBounds().clone();
							enlargedBounds.scaleCOG( this.enlargeBoundsAmount );
							sameAs.bestFaceImage = image.extractROI( enlargedBounds );
						}
					}

					// Cache the face track in a HashMap
					TreeMap<Integer, TrackInfo> track = currentTracks.get( sameAs );
					if( track == null )
						currentTracks.put( sameAs, track = new TreeMap<Integer,TrackInfo>() );
					track.put( xv.getCurrentFrameIndex(), new TrackInfo( face.getBounds().clone(), score ) );

					if( VideoFaceTracker.IMAGE_DEBUG )
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

					if( this.cacheImages )
						this.cacheImage( video, face );

					// Output the face tracks
					if( this.cacheFaceTracks )
						this.outputFaceTracks( video, face, currentTracks.get(face) );
				}
				// We'll ignore this face if it wasn't around long enough
				else
					if( VideoFaceTracker.DEBUG )
						System.out.println( "Discarding "+face );

				// Remove this face from the tracked faces list.
				this.frameCount.remove( face );
				currentTracks.remove( face );
			}

			if( VideoFaceTracker.IMAGE_DEBUG )
			{
				DisplayUtilities.displayName( image, "video" );
				this.displayPatches();
			}

			// Store the last frame timecode.
			lastFrameTimecode = xv.getCurrentTimecode();
		}
	}

	/**
	 * 	Outputs the face tracks for a given face to a file.
	 *	@param face
	 *	@param treeMap
	 */
	private void outputFaceTracks( final File video, final FaceRange face, final TreeMap<Integer, TrackInfo> treeMap )
	{
		// Make sure the cache directory exists.
		final File cDir = new File( this.cacheDir + video.getName() );
		cDir.mkdirs();

		// The name of the file will be the timecode start and end
		final String timeString =
			String.format( "%06d", face.start.getFrameNumber() ) + "-" +
			String.format( "%06d", face.end.getFrameNumber() );

		try
		{
			// Write each rectangle to a file
			final FileWriter fw = new FileWriter( new File( cDir, timeString+"-tracks.txt" ) );

			for( final Entry<Integer, TrackInfo> e : treeMap.entrySet() )
				fw.write( e.getKey()+" : "+e.getValue().score + " : "+e.getValue().bounds.toString() + "\n" );

			fw.close();
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * 	Puts the given faces into a
	 * 	@param video
	 *	@param face
	 */
	private void cacheImage( final File video, final FaceRange face )
	{
		// Make sure the cache directory exists.
		final File cDir = new File( this.cacheDir + video.getName() );
		cDir.mkdirs();

		// The name of the file will be the timecode start and end
		final String timeString =
			String.format( "%06d", face.start.getFrameNumber() ) + "-" +
			String.format( "%06d", face.end.getFrameNumber() );

		// Write the image to a file
		final File outputFile = new File( cDir, timeString+"-face.png" );
		try
		{
			ImageUtilities.write( face.bestFaceImage, "png", outputFile );
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}

		// If we don't want to store face images in memory, we'll remove
		// them from the object here.
		if( !this.storeFaceImages )
		{
			face.bestFaceImage = null;
		}
	}

	/**
	 * 	Caches a show boundary keyframe.
	 *	@param video
	 *	@param frameNumber
	 *	@param kf
	 */
	private void cacheShotBoundary( final File video, final int frameNumber, final VideoKeyframe<MBFImage> kf )
	{
		// Make sure the cache directory exists.
		final File cDir = new File( this.cacheDir + video.getName() );
		cDir.mkdirs();

		// The name of the file will be the timecode start and end
		final String timeString =
			String.format( "%06d", frameNumber );

		// Write the image to a file
		final File outputFile = new File( cDir, timeString+"-shot-boundary.png" );
		try
		{
			ImageUtilities.write( kf.getImage(), "png", outputFile );
		}
		catch( final IOException e )
		{
			e.printStackTrace();
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

	/**
	 * 	Calculate the face score.
	 *
	 *	@param face The face
	 *	@param frame The original frame
	 *	@return The score
	 */
	private double calculateScore( final DetectedFace face, final MBFImage frame )
	{
		if( face instanceof CLMDetectedFace )
		{
			// The assumption is, of course, that the tracker is
			// tracking the pose well. In reality, it actually doesn't
			// do it very well. Never-the-less, we'll assume here that
			// it does.  We attempt to create a formula that promotes
			// a central pose and a large image.
			final CLMDetectedFace f = (CLMDetectedFace)face;

			// A central pose has the yaw and pitch close to zero.
			// (zero will be the average pose of the training sets used).
			// With our current model, the pitch and yaw need to be within
			// 0.5 from the mean to be useful for us (determined just by
			// eye-balling the values from the ModelManipulatorGUI)
			final double y = f.getPitch();	// look up/down
			final double x = f.getYaw(); 	// look side-to-side

			// The size also matters. We want a large as image as possible.
			final double area = face.getBounds().calculateArea();

			// Converting the pitch and yaw to a score (from a distance):
			//		p = 0.5 - Math.abs(pitch)
			//		y = 0.5 - Math.abs(yaw)
			// and we clip the score at 0 (so it can't be negative). We scale
			// then 0-1 and then multiply them together to get a score for the pose.
			//
			// We then multiply that score by the area of the bounds of the face
			// (weighted by some configurable constant, alpha) so that we promote
			// large, good-pose faces.
			final double alpha = 1;
			final double score =
					Math.max( 0, 0.5 - Math.abs( y ) )*2 *
					Math.max( 0, 0.5 - Math.abs( x ) )*2 *
					alpha * area;

			return score;
		}

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
	 * 	Get the current pixel aspect ratio.
	 *	@return The current pixel aspect ratio
	 */
	public double getAspect()
	{
		return this.aspect;
	}

	/**
	 * 	Set the pixel aspect ratio
	 *	@param aspect The aspect ratio
	 */
	public void setAspect( final double aspect )
	{
		this.aspect = aspect;
	}

	/**
	 *	Get the directory to where images are being cached.
	 *	@return The cache dir.
	 */
	public String getCacheDir()
	{
		return this.cacheDir;
	}

	/**
	 * 	Set the directory to where images will be cached.
	 *	@param cacheDir The cache dir
	 */
	public void setCacheDir( final String cacheDir )
	{
		this.cacheDir = cacheDir;
	}

	/**
	 *	@return the cacheImages
	 */
	public boolean isCacheImages()
	{
		return this.cacheImages;
	}

	/**
	 *	@param cacheImages the cacheImages to set
	 */
	public void setCacheImages( final boolean cacheImages )
	{
		this.cacheImages = cacheImages;
	}

	/**
	 *	@return the deinterlaceVideo
	 */
	public boolean isDeinterlaceVideo()
	{
		return this.deinterlaceVideo;
	}

	/**
	 *	@param deinterlaceVideo the deinterlaceVideo to set
	 */
	public void setDeinterlaceVideo( final boolean deinterlaceVideo )
	{
		this.deinterlaceVideo = deinterlaceVideo;
	}

	/**
	 *	@return the storeFaceImages
	 */
	public boolean isStoreFaceImages()
	{
		return this.storeFaceImages;
	}

	/**
	 *	@param storeFaceImages the storeFaceImages to set
	 */
	public void setStoreFaceImages( final boolean storeFaceImages )
	{
		this.storeFaceImages = storeFaceImages;
	}

	/**
	 *	@return the downscale
	 */
	public double getDownscale()
	{
		return this.downscale;
	}

	/**
	 *	@param downscale the downscale to set
	 */
	public void setDownscale( final double downscale )
	{
		this.downscale = downscale;
	}

	/**
	 *	@return the nSkipFrames
	 */
	public int getnSkipFrames()
	{
		return this.nSkipFrames;
	}

	/**
	 *	@param nSkipFrames the nSkipFrames to set
	 */
	public void setnSkipFrames( final int nSkipFrames )
	{
		this.nSkipFrames = nSkipFrames;
	}

	/**
	 *	@return the cacheFaceTracks
	 */
	public boolean isCacheFaceTracks()
	{
		return this.cacheFaceTracks;
	}

	/**
	 *	@param cacheFaceTracks the cacheFaceTracks to set
	 */
	public void setCacheFaceTracks( final boolean cacheFaceTracks )
	{
		this.cacheFaceTracks = cacheFaceTracks;
	}

	/**
	 *	@return the shotBoundaryThresholdRatio
	 */
	public double getShotBoundaryThresholdRatio()
	{
		return this.shotBoundaryThresholdRatio;
	}

	/**
	 *	@param shotBoundaryThresholdRatio the shotBoundaryThresholdRatio to set
	 */
	public void setShotBoundaryThresholdRatio( final double shotBoundaryThresholdRatio )
	{
		this.shotBoundaryThresholdRatio = shotBoundaryThresholdRatio;
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
		vft.setAspect( 1.77777777778 );	// 16:9
		vft.processVideo( videoFile );
	}
}
