/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013.dpd;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import mx.bigdata.jcalais.CalaisClient;
import mx.bigdata.jcalais.CalaisObject;
import mx.bigdata.jcalais.CalaisResponse;
import mx.bigdata.jcalais.rest.CalaisRestClient;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import org.openimaj.image.DisplayUtilities.ImageComponent;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.OpenCalaisAPIToken;
import org.openimaj.util.pair.IndependentPair;

import com.google.common.io.Files;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * Person recogniser tool. Given the base filename of a video (i.e. without the
 * extension) it will analyse the metadata for names, train a classifier for
 * recognising those people (by searching Bing for the name of the person along
 * with the name of the programme), and then analyse the video to find the best
 * faces of people in the videos and compare them with the classifier. The main
 * method is the {@link #process()} method which contains pretty much all the
 * logic.
 * <p>
 * <b>Note</b>: The defaults for many of the directories within the class probably
 * won't work for you!  Use the setters to set the directories up before processing
 * a video.
 *
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * @created 18 Jul 2013
 * @version $Author$, $Revision$, $Date$
 */
public class PersonRecogniser
{
	/** The person model for OpenNLP */
	private static final String OPENNLP_PERSON_MODEL = "/org/openimaj/text/opennlp/models/en-ner-person.bin";

	/** The metadata directory where the programming information json files reside */
	private String metadataDir = "/data/degas/mediaeval/mediaeval-searchhyper/collection/metadata/";

	/** The directory where the videos reside */
	private String videoDir = "/data/degas/mediaeval/mediaeval-searchhyper/collection/videos/";

	/** The directory where the videos reside */
	private String subtitlesDir = "/data/degas/mediaeval/mediaeval-searchhyper/collection/subtitles/xml/";

	/**
	 * The directory where the cached face images will be written to and read
	 * from
	 */
	private String cacheDir = "/home/dd/Dropbox/Work/mediaeval/faces/cache/";

	/** The extension for the video file */
	private String videoExtension = ".webm";

	/** The base filename of the video/metadata/cache files and directories */
	private String baseFilename;

	/** The video aspect ratio */
	private double videoAspect = 1;

	/**
	 * Construct a person recogniser using the base filename of the video. It is
	 * expected that the video, metadata and face cache directory will all have
	 * the base name given here. By default the video is expected to have a
	 * <code>.webm</code> extension, but that can be changed with
	 * {@link #setVideoExtension(String)}.
	 *
	 * @param baseFilename The base filename of the video and metadata files
	 */
	public PersonRecogniser( final String baseFilename ) throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		this.baseFilename = baseFilename;
	}

	/**
	 * Process the video with the given base filename. See constructor for
	 * information about the base filename.
	 *
	 * @param baseFilename The base filename
	 * @throws Exception If an error occurs during processing
	 */
	public void process( final String baseFilename ) throws Exception
	{
		this.baseFilename = baseFilename;
		this.process();
	}

	/**
	 * Process the video.
	 *
	 * @throws Exception If an error occurs during processing.
	 */
	public void process() throws Exception
	{
		// Put the files together.
		// First the programme metadata file...
		final File programmeInfoFile = new File( this.metadataDir + this.baseFilename + ".json" );

		// ...the subtitles file...
		final File subtitlesFile = new File( this.subtitlesDir + this.baseFilename + ".xml" );

		// .. and the video itself
		final File videoFile = new File( this.videoDir + this.baseFilename + this.videoExtension );

		try
		{
			// Parse the programme metadata into a ProgrammeInfo object
			final ProgrammeInfo pi = ProgrammeInfo.create( programmeInfoFile );

			// Parse the programme subtitles into a ProgrammeSubtitles object
			final ProgrammeSubtitles ps = ProgrammeSubtitles.create( subtitlesFile );

			// Analyse the description for the people names
			final Set<String> peopleNames = this.getPeopleNames( pi, ps );

			System.out.println( "Found these people:\n\t"+peopleNames );

			// Create a classifier for the given people names
			PersonMatcher pm = null;
			final File recFile = new File( this.baseFilename + ".rec" );
			if( recFile.exists() )
					pm = new PersonMatcher( recFile );
			else	pm = new PersonMatcher( peopleNames, recFile, true );

			// These options are for minimising false positives in images with
			// multiple people in them. We only have images with a single person
			// in it.
			pm.setAllowOnlyOneInstance( false );
			pm.setIgnoreBlurredFaces( false );

			// Images are being cached by the video tracker, so
			// we don't need to do it if the directory for the given video
			// already exists in the cache location.
			final File videoFaceCacheDir = new File( this.cacheDir + this.baseFilename + ".webm/" );
			if( !videoFaceCacheDir.exists() )
			{
				// Get the video face tracker
				final VideoFaceTracker vft = new VideoFaceTracker();

				// We'll cache images to disk, but not to memory
				vft.setStoreFaceImages( false );
				vft.setCacheImages( true );
				vft.setCacheDir( this.cacheDir );
				vft.setnSkipFrames( 3 );

				// We'll set the video aspect ratio
				vft.setAspect( this.videoAspect );

				// Process the video to find the images of people in the video
				vft.processVideo( videoFile );
			}
			else
				System.out.println( "Cache for video already exists. Using cache." );

			// Read the list of images back in from the cache. We use the cache
			// so that
			// we can deal with more images than will fit in memory.
			final File[] files = videoFaceCacheDir.listFiles( new FileFilter()
			{
				@Override
				public boolean accept( final File pathname )
				{
					return pathname.getName().endsWith( ".png" );
				}
			} );

			// Now loop over the found faces and match them against the
			// person classifier we created just a minute ago
			for( final File f : files )
			{
				final MBFImage img = ImageUtilities.readMBF( f );

				final List<? extends IndependentPair<? extends DetectedFace, ScoredAnnotation<String>>> x
						= pm.query( img.flatten() );

				// As the query image should only have one face within it, the
				// hope is that there will only be a single result in the person
				// matcher.
				if( x.size() > 0 )
				{
					// Get the single result
					final IndependentPair<? extends DetectedFace, ScoredAnnotation<String>> y = x.get( 0 );
					final String name = (y.secondObject() == null ? "Unknown" : y.secondObject().annotation);

					// Write the name of the person to a file next to the face
					// image.
					final FileWriter fw = new FileWriter( new File( f.getCanonicalPath() + ".txt" ) );
					fw.write( name );
					fw.close();
				}
			}

			this.showResults( files );
		}
		catch( final Exception e )
		{
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 	Replaces all and numbers punctuation in a string with a space. Rather than removing
	 * 	it replaces to avoid words merging (e.g. "david...john?" might become "davidjohn" instead
	 * 	becomes "david    john ") and also keeps the character offsets the same in the text.
	 *
	 *	@param text The text
	 *	@return
	 */
	public String cleanString( final String text )
	{
		return text.replaceAll("[^\\p{L} ']", " ");
	}

	/**
	 * Given a {@link ProgrammeInfo} and {@link ProgrammeSubtitles},
	 * it will analyse the description and return
	 * a list of people names involved in the programme.
	 *
	 * @param pi The {@link ProgrammeInfo}
	 * @param ps The {@link ProgrammeSubtitles}
	 * @return A list of people names
	 */
	private Set<String> getPeopleNames( final ProgrammeInfo pi, final ProgrammeSubtitles ps )
	{
		final HashSet<String> people = new HashSet<String>();

		people.addAll( this.getPeopleNames( this.cleanString( pi.description ) ) );

		// For the subtitles text we will have to chop it down and process it bit by bit
		final int maxLineLength = 1024;
		final String[] lines = ps.getText().split( "\n." );
		String currentLine = "";
		for( final String line : lines )
		{
			if( currentLine.length() + line.length() < maxLineLength )
					currentLine += this.cleanString( line )+"\n";
			else
			{
				people.addAll( this.getPeopleNames( currentLine ) );
				currentLine = this.cleanString( line )+"\n";
			}
		}

		return people;
	}

	/**
	 * 	Analyses the given text for people's names using both OpenCalais and OpenNLP.
	 *
	 *	@param text The text
	 *	@return A list of people
	 */
	private Set<String> getPeopleNames( final String text )
	{
		final Set<String> peopleNames = new HashSet<String>();

		try
		{
			// ----------------------------------------------------
			// Connect to the OpenCalais service
			final OpenCalaisAPIToken token = DefaultTokenFactory.get( OpenCalaisAPIToken.class );
			final CalaisClient cc = new CalaisRestClient( token.apiKey );
			final CalaisResponse a = cc.analyze( text );

			// Get the names of the people out
			for( final CalaisObject x : a.getEntities() )
				if( x.getField( "commonname" ) != null )
					peopleNames.add( x.getField( "commonname" ) );

			// ----------------------------------------------------
			// Now do the same for OpenNLP
			try
			{
				// Get the model
				final InputStream modelStream = this.getClass().getResourceAsStream( PersonRecogniser.OPENNLP_PERSON_MODEL );
				final TokenNameFinderModel model = new TokenNameFinderModel( modelStream );

				// Get the name finder
				final NameFinderME finder = new NameFinderME( model );

				// OpenNLP wants each token separately
				final String[] splits = text.split( "\\s" );

				// Find people in the description
				final Span[] spans = finder.find( splits );

				// Now loop through the spans and recreate the name
				for( final Span span : spans )
				{
					String name = "";
					for( int x = span.getStart(); x < span.getEnd(); x++ )
						if( splits[x] != null )
							name += splits[x] + " ";

					// Note that if it finds the same names as OpenCalais, then
					// the HashSet will overwrite the name
					peopleNames.add( name.trim() );
				}
			}
			catch( final Exception e )
			{
				e.printStackTrace();
			}
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}

		return peopleNames;
	}

	/**
	 * @return the metadataDir
	 */
	public String getMetadataDir()
	{
		return this.metadataDir;
	}

	/**
	 * @param metadataDir the metadataDir to set
	 */
	public void setMetadataDir( final String metadataDir )
	{
		this.metadataDir = metadataDir;
	}

	/**
	 * @return the videoDir
	 */
	public String getVideoDir()
	{
		return this.videoDir;
	}

	/**
	 * @param videoDir the videoDir to set
	 */
	public void setVideoDir( final String videoDir )
	{
		this.videoDir = videoDir;
	}

	/**
	 * @return the cacheDir
	 */
	public String getCacheDir()
	{
		return this.cacheDir;
	}

	/**
	 * @param cacheDir the cacheDir to set
	 */
	public void setCacheDir( final String cacheDir )
	{
		this.cacheDir = cacheDir;
	}

	/**
	 * @return the videoExtension
	 */
	public String getVideoExtension()
	{
		return this.videoExtension;
	}

	/**
	 * @param videoExtension the videoExtension to set
	 */
	public void setVideoExtension( final String videoExtension )
	{
		this.videoExtension = videoExtension;
	}

	/**
	 * @return the videoAspect
	 */
	public double getVideoAspect()
	{
		return this.videoAspect;
	}

	/**
	 * @param videoAspect the videoAspect to set
	 */
	public void setVideoAspect( final double videoAspect )
	{
		this.videoAspect = videoAspect;
	}

	/**
	 * Given the list of caches faces, will display them with their recognised
	 * name and stats.
	 *
	 * @param files The files
	 */
	public void showResults( final File[] files )
	{
		// Create a new image component to display the results
		final ImageComponent ic = new ImageComponent();
		ic.setAllowZoom( false ); // We need to disable this

		final JFrame f = new JFrame( "Results" );
		f.getContentPane().add( ic );
		f.setSize( new Dimension( 400, 400 ) );
		f.setVisible( true );

		// Add a mouse listener
		ic.addMouseListener( new MouseAdapter()
		{
			int fc = 0;

			@Override
			public void mouseClicked( final MouseEvent event )
			{
				if( event.getX() > f.getSize().width / 2 ) if( this.fc < files.length - 1 )
					this.fc++;
				else if( this.fc > 0 ) this.fc--;

				try
				{
					String str;
					try
					{
						str = Files.readFirstLine( new File( files[this.fc].getCanonicalPath() + ".txt" ), Charset.forName( "UTF-8" ) );
					}
					catch( final Exception e )
					{
						e.printStackTrace();
						str = "Not processed.";
					}

					final BufferedImage bf = ImageIO.read( files[this.fc] );
					bf.getGraphics().drawString( str, 10, bf.getHeight() - 10 );
					ic.setImage( bf );
				}
				catch( final IOException e )
				{
					e.printStackTrace();
				}
			};
		} );
	}

	/**
	 *	@return the subtitlesDir
	 */
	public String getSubtitlesDir()
	{
		return this.subtitlesDir;
	}

	/**
	 *	@param subtitlesDir the subtitlesDir to set
	 */
	public void setSubtitlesDir( final String subtitlesDir )
	{
		this.subtitlesDir = subtitlesDir;
	}

	/**
	 * @param args
	 */
	public static void main( final String[] args )
	{
		if( args.length < 1 )
		{
			System.err.println( "Please supply the base file of a video you want to analyse." );
			System.exit( 1 );
		}

		try
		{
			// Create a person recogniser with the command-line video
			final PersonRecogniser pr = new PersonRecogniser( args[0] );

			// BBC videos are stored 16:9 anamorphic
			pr.setVideoAspect( 1.7777777777 );

			// Process the video.
			pr.process();
		}
		catch( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
