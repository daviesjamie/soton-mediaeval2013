/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mx.bigdata.jcalais.CalaisClient;
import mx.bigdata.jcalais.CalaisObject;
import mx.bigdata.jcalais.CalaisResponse;
import mx.bigdata.jcalais.rest.CalaisRestClient;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.OpenCalaisAPIToken;
import org.openimaj.util.pair.IndependentPair;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 *	Person recogniser tool. Given the base filename of a video (i.e. without the extension)
 * 	it will analyse the metadata for names, train a classifier for recognising those people
 * 	(by searching Bing for the name of the person along with the name of the programme),
 * 	and then analyse the video to find the best faces of people in the videos and compare
 * 	them with the classifier.
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 18 Jul 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class PersonRecogniser
{
	public static String METADATA_DIR = "/data/degas/mediaeval/mediaeval-searchhyper/collection/metadata/";
	public static String VIDEO_DIR = "/data/degas/mediaeval/mediaeval-searchhyper/collection/videos/";
	public static String FACE_CACHE_DIR = "/home/dd/Dropbox/Work/mediaeval/faces/cache/";

	public PersonRecogniser( final String baseFilename )
			throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		// Put the files together.
		// First the programme metadata file
		final File programmeInfoFile = new File( 
				PersonRecogniser.METADATA_DIR+baseFilename+".json" );
		
		// .. and the video itself
		final File videoFile = new File( 
				PersonRecogniser.VIDEO_DIR + baseFilename + ".webm" );
		
		try
		{
			// Parse the programme metadata into a ProgrammeInfo object
			final ProgrammeInfo pi = ProgrammeInfo.create( programmeInfoFile );
			
			// Analyse the description for the people names
			final List<String> peopleNames = this.getPeopleNames( pi );
			
			// Create a classifier for the given people names
			PersonMatcher pm = null;
			File recFile = new File(baseFilename+".rec");
			if( recFile.exists() )
					pm = new PersonMatcher( recFile );
			else	pm = new PersonMatcher( peopleNames, recFile, true );
			
			// These options are for minimising false positives in images with
			// multiple people in them. We only have images with a single person in it.
			pm.setAllowOnlyOneInstance( false );
			pm.setIgnoreBlurredFaces( false );
			
			// Images are being cached by the video tracker, so
			// we don't need to do it if the directory for the given video
			// already exists in the cache location.
			File videoFaceCacheDir = new File( FACE_CACHE_DIR + baseFilename + ".webm/" );
			if( !videoFaceCacheDir.exists() )
			{
				// Process the video to find the images of people in the video
				VideoFaceTracker vft = new VideoFaceTracker();
				vft.setCacheDir( FACE_CACHE_DIR );
				vft.setAspect( 1.77777777777 );
				vft.processVideo( videoFile );
			}
			else	System.out.println( "Cache for video already exists. Using cache." );
			
			// Now loop over the found faces and match them against the
			// person classifier we created just a minute ago
			File[] files = videoFaceCacheDir.listFiles( new FileFilter()
			{
				@Override
				public boolean accept( File pathname )
				{
					return pathname.getName().endsWith( ".png" );
				}
			} );

			for( File f : files )
			{
				MBFImage img = ImageUtilities.readMBF( f );
				
				List<? extends IndependentPair<? extends DetectedFace, ScoredAnnotation<String>>> 
					x = pm.query( img.flatten() );
				
				// As the query image should only have one face within it, the
				// hope is that there will only be a single result in the person
				// matcher.
				if( x.size() > 0 )
				{
					IndependentPair<? extends DetectedFace, ScoredAnnotation<String>> y = x.get(0);
					final String name = (y.secondObject() == null ? 
							"Unknown" : y.secondObject().annotation);

					FileWriter fw = new FileWriter( new File( f.getCanonicalPath()+".txt" ) );
					fw.write( name );
					fw.close();
				}
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	/**
	 *	Given a {@link ProgrammeInfo}, it will analyse the description and return
	 *	a list of people names involved in the programme.
	 *
	 *	@param pi The {@link ProgrammeInfo}
	 *	@return A list of people names
	 */
	private List<String> getPeopleNames( final ProgrammeInfo pi )
	{
		List<String> peopleNames = new ArrayList<String>();
		try
		{
			// Connect to the OpenCalais service
			final OpenCalaisAPIToken token = DefaultTokenFactory.get( OpenCalaisAPIToken.class );
			final CalaisClient cc = new CalaisRestClient( token.apiKey );
			final CalaisResponse a = cc.analyze( pi.description );

			// Get the names of the people out
			for( CalaisObject x : a.getEntities() )
				peopleNames.add( x.getField( "commonname" ) );
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}

		return peopleNames;
	}

	/**
	 *	@param args
	 */
	public static void main( final String[] args )
	{
		if( args.length < 1 )
		{
			System.err.println( "Please supply the base file of a video you want to analyse.");
			System.exit(1);
		}

		try
		{
			new PersonRecogniser( args[0] );
		}
		catch( final JsonSyntaxException e )
		{
			e.printStackTrace();
		}
		catch( final JsonIOException e )
		{
			e.printStackTrace();
		}
		catch( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
	}
}
