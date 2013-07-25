/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *	Encapsulates programme subtitles/timed text.
 *
 * 	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 * 	@created 25 Jul 2013
 */
public class ProgrammeSubtitles
{
	/**
	 * 	Representing one line of timed text.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 25 Jul 2013
	 */
	public static class TimedTextLine
	{
		public String text = "";
		public long start;
		public long end;

		@Override
		public String toString()
		{
			return this.start + " -> " + this.end +" : "+this.text ;
		}
	}

	/** The subtitles */
	private final List<TimedTextLine> lines = new ArrayList<TimedTextLine>();

	/**
	 * 	Returns the whole text of the subtitles as a single string.
	 *	@return The text of the subtitles
	 */
	public String getText()
	{
		final StringBuilder sb = new StringBuilder();
		for( final TimedTextLine line : this.lines )
			sb.append( line.text ).append( " " );
		return sb.toString();
	}

	/**
	 * 	Create a new subtitles object from the given file.
	 *	@param subtitlesFile The subtitles file
	 */
	public static ProgrammeSubtitles create( final File subtitlesFile )
	{
		final ProgrammeSubtitles ps = new ProgrammeSubtitles();

		try
		{
			final XMLReader xr = XMLReaderFactory.createXMLReader();
			xr.setContentHandler( new DefaultHandler()
			{
				private TimedTextLine ttl;

				/**
				 *	{@inheritDoc}
				 * 	@see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
				 */
				@Override
				public void startElement( final String uri, final String localName,
						final String qName, final Attributes attributes ) throws SAXException
				{
					if( localName.equals( "p" ) )
					{
						this.ttl = new TimedTextLine();
						this.ttl.start = (long)(ImportUtils.HMStoS( attributes.getValue( "begin" ) )*1000);
						this.ttl.end   = (long)(ImportUtils.HMStoS( attributes.getValue( "end" ) )*1000);
					}
				}

				/**
				 *	{@inheritDoc}
				 * 	@see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
				 */
				@Override
				public void characters( final char[] ch, final int start, final int length ) throws SAXException
				{
					if( this.ttl != null )
					{
						this.ttl.text += String.valueOf( ch, start, length );
					}
				}

				/**
				 *	{@inheritDoc}
				 * 	@see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
				 */
				@Override
				public void endElement(final String uri, final String localName, final String qName) throws SAXException
				{
					if( localName.equals("p") )
					{
						ps.lines.add( this.ttl );
					}
				};
			} );

			// ================================================== //
			// Try and parse the subtitles XML into the list
			// ================================================== //
			try
			{
				xr.parse( new InputSource( new FileReader( subtitlesFile ) ) );
			}
			catch( final FileNotFoundException e )
			{
				e.printStackTrace();
			}
			catch( final IOException e )
			{
				e.printStackTrace();
			}
			catch( final SAXException e )
			{
				e.printStackTrace();
			}
		}
		catch( final SAXException e )
		{
			e.printStackTrace();
		}

		return ps;
	}

	/**
	 * @param args
	 */
	public static void main( final String[] args )
	{
		final ProgrammeSubtitles ps = ProgrammeSubtitles.create(
			new File( "/data/degas/mediaeval/mediaeval-searchhyper/collection/" +
					"subtitles/xml/20080401_010000_bbcfour_the_book_quiz.xml") );

		System.out.println( ps.getText() );
	}
}
