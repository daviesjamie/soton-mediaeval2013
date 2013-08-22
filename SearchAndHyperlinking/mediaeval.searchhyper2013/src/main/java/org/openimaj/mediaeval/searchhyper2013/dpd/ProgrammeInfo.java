/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013.dpd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 *	Uses GSON to read the programme information from the metadata JSON file
 *	associated with a programme.
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 18 Jul 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class ProgrammeInfo
{
	public String diskref;
	public String service;
	public String variant;
	public String date;
	public String time;
	public String duration;
	public String uri;
	public String series_uri;
	public String canonical;
	public String depiction;
	public HashMap<String,String> depictions;
	public String title;
	public String description;
	public String original_description;
	public boolean subtitles;
	public boolean signed;
	public boolean hd;
	public boolean repeat;
	public String pcrid;
	public String scrid;
	public String when;
	public String id;
	public String filename;
	public String type;
	public String source;
	public String service_name;
	public String uuid;
	public int original_network_id;
	public int service_id;
	public int  event_id;
	public String dvb_uri;
	public boolean ad;
	public HashMap<String,String> broadcast;
	public HashMap<String,String> episode;
	public HashMap<String,String> series;
	public HashMap<String,String> brand;
	public HashMap<String,String> version;
	public String key;
	public HashMap<String,HashMap<String,String>> media;

	public ProgrammeInfo()
	{
	}

	public static ProgrammeInfo create( final File jsonFile )
			throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		return new Gson().fromJson( new FileReader( jsonFile ), ProgrammeInfo.class );
	}

	/**
	 *	@param args
	 */
	public static void main( final String[] args )
	{
		if( args.length < 1 )
		{
			System.err.println( "Please supply a json file." );
			System.exit(1);
		}

		try
		{
			final ProgrammeInfo p = ProgrammeInfo.create( new File(args[0]) );
			System.out.println( "This info is about "+p.title+" which aired at "+p.time+" on "+p.service );
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
