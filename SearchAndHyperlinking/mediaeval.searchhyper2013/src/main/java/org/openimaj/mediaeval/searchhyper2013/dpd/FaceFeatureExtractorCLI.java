/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013.dpd;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.feature.FacePatchFeature;
import org.openimaj.image.processing.face.feature.FacePatchFeature.Extractor;

/**
 * Writes face patch features to all image files in a given directory.
 *
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * @created 21 Aug 2013
 * @version $Author$, $Revision$, $Date$
 */
public class FaceFeatureExtractorCLI
{
	private static boolean recurse = true;

	/**
	 * The main method
	 *
	 * @param args Requires a directory of images to be provided.
	 * @throws FileSystemException
	 */
	public static void main( final String[] args ) throws FileSystemException
	{
		// Check a directory is given
		if( args.length == 0 )
		{
			System.err.println( "Please provide a directory of images." );
			System.exit( 1 );
		}

		// Get the list of image files
		final File[] files = new File( args[0] ).listFiles( new FileFilter()
		{
			@Override
			public boolean accept( final File pathname )
			{
				return pathname.getName().endsWith( ".jpg" ) || pathname.getName().endsWith( ".png" )
						|| (pathname.isDirectory() && !pathname.getName().equals( ".." ) &&
							!pathname.getName().equals(".") );
			}
		} );

		// Loop through the image file...
		for( final File f : files )
		{
			if( f.isDirectory() && FaceFeatureExtractorCLI.recurse )
			{
				FaceFeatureExtractorCLI.main( new String[] { f.getAbsolutePath() } );
				continue;
			}

			// Check if the feature already exists...
			final File ff = new File( f.getAbsolutePath() + ".facepatchfeature.fv" );
			if( !ff.exists() )
			{
				// If not, we'll generate it.
				try
				{
					// Read the image
					System.out.println( "Reading " + f );
					final MBFImage img = ImageUtilities.readMBF( f );

					// Detect faces
					final FKEFaceDetector fkd = new FKEFaceDetector();
					final List<KEDetectedFace> faces = fkd.detectFaces( img.flatten() );

					// There should only be one.
					if( faces.size() > 0 )
					{
						// Choose the first face...
						System.out.println( "    - Found " + faces.size() + " faces." );
						final KEDetectedFace face = faces.get( 0 );

						// Extract the face patch
						final Extractor fpfe = new FacePatchFeature.Extractor();
						final FacePatchFeature fpf = fpfe.extractFeature( face );

						// Write the feature vector to a file
						System.out.println( "    - Writing " + ff );
						final DataOutput out = new DataOutputStream( new FileOutputStream( ff ) );
						fpf.writeBinary( out );
					}
				}
				catch( final IOException e )
				{
					e.printStackTrace();
				}
			}
		}
	}
}
