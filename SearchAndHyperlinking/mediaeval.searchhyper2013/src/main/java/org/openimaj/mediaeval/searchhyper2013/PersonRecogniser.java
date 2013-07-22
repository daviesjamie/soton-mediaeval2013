/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import mx.bigdata.jcalais.CalaisClient;
import mx.bigdata.jcalais.CalaisResponse;
import mx.bigdata.jcalais.rest.CalaisRestClient;

import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.OpenCalaisAPIToken;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 *
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 18 Jul 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class PersonRecogniser
{
	public static String METADATA_DIR = "/data/degas/mediaeval/mediaeval-searchhyper/collection/metadata/";
	public static String VIDEO_DIR = "/data/degas/mediaeval/mediaeval-searchhyper/collection/videos/";

	public PersonRecogniser( final String baseFilename )
			throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		final File programmeInfoFile = new File( PersonRecogniser.METADATA_DIR+baseFilename+".json" );
		final ProgrammeInfo pi = ProgrammeInfo.create( programmeInfoFile );
		final List<String> peopleNames = this.getPeopleNames( pi );
	}

	/**
	 *
	 *	@param pi
	 *	@return
	 */
	private List<String> getPeopleNames( final ProgrammeInfo pi )
	{
		try
		{
			// Connect to the OpenCalais service
			final OpenCalaisAPIToken token = DefaultTokenFactory.get( OpenCalaisAPIToken.class );
			final CalaisClient cc = new CalaisRestClient( token.apiKey );
			final CalaisResponse a = cc.analyze( pi.description );

			System.out.println( a );
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}

		return null;
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
