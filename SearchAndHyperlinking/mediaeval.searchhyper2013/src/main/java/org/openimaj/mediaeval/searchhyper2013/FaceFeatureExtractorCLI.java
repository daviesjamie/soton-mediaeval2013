/**
 * 
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.feature.FloatFV;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.feature.FacePatchFeature;
import org.openimaj.image.processing.face.feature.FacePatchFeature.Extractor;

/**
 *	
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 21 Aug 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class FaceFeatureExtractorCLI
{
	public static void main( String[] args ) throws FileSystemException
	{
		if( args.length == 0 )
		{
			System.err.println( "Please provide a directory of images." );
			System.exit(1);
		}

		File[] files = new File( args[0] ).listFiles( new FilenameFilter()
		{
			
			@Override
			public boolean accept( File dir, String name )
			{
				return name.endsWith( ".jpg" ) || name.endsWith( ".png" );
			}
		} );
		
		for( File f : files )
		{
			File ff = new File( f.getAbsolutePath()+".facepatchfeature.fv" );
			if( !ff.exists() )
			{
				try
				{
					System.out.println( "Reading "+f );
					MBFImage img = ImageUtilities.readMBF( f );
					
					FKEFaceDetector fkd = new FKEFaceDetector();
					List<KEDetectedFace> faces = fkd.detectFaces( img.flatten() );
					
					// There should only be one.
					if( faces.size() > 0 )
					{
						System.out.println( "    - Found "+faces.size()+" faces.");
						KEDetectedFace face = faces.get( 0 );
						
						Extractor fpfe = new FacePatchFeature.Extractor();
						FacePatchFeature fpf = fpfe.extractFeature( face );
						
						FloatFV fv = fpf.getFeatureVector();
						
						System.out.println( "    - Writing "+ff );
						DataOutput out = new DataOutputStream( new FileOutputStream( ff ) );
						fv.writeBinary( out  );
					}
				}
				catch( IOException e )
				{
					e.printStackTrace();
				}
		}
		}
	}
}
