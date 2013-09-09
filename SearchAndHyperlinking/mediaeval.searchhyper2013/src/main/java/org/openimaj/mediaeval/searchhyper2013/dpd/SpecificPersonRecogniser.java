/**
 *
 */
package org.openimaj.mediaeval.searchhyper2013.dpd;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.FloatFV;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.feature.FacePatchFeature;
import org.openimaj.image.processing.face.feature.FacePatchFeature.Extractor;
import org.openimaj.io.InputStreamObjectReader;
import org.openimaj.ml.annotation.Annotated;
import org.openimaj.ml.annotation.AnnotatedObject;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.ml.annotation.svm.SVMAnnotator;

/**
 *	Implementation of Parkhi et.al Specific Person Retrieval
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 21 Aug 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class SpecificPersonRecogniser
{
	/**
	 * 	An object reader for face patch features.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 21 Aug 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	public static class FacePatchFeatureReader
		implements InputStreamObjectReader<FacePatchFeature>
	{
		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.io.ObjectReader#read(java.lang.Object)
		 */
		@Override
		public FacePatchFeature read( final InputStream source ) throws IOException
		{
			final FacePatchFeature fpf = new FacePatchFeature();
			fpf.readBinary( new DataInputStream( source ) );
			return fpf;
		}

		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.io.ObjectReader#canRead(java.lang.Object, java.lang.String)
		 */
		@Override
		public boolean canRead( final InputStream source, final String name )
		{
			return name.endsWith( ".facepatchfeature.fv" );
		}
	}

	/**
	 * 	Wrapper to get the feature vector from the feature implementation.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 21 Aug 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	public static class FaceFV implements FeatureExtractor<FloatFV,FacePatchFeature>
	{
		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.feature.FeatureExtractor#extractFeature(java.lang.Object)
		 */
		@Override
		public FloatFV extractFeature( final FacePatchFeature object )
		{
			return object.getFeatureVector();
		}
	}

	/**
	 * 	Represents a single person classifier.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 21 Aug 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	public static class PersonClassifier
	{
		/** The actual classifier */
		public SVMAnnotator<FacePatchFeature, String> classifier;

		/** The negatives */
		private final Collection<Annotated<FacePatchFeature, String>> negativeDataset;

		/**
		 * 	Construct a person classifier
		 */
		public PersonClassifier( final Collection<Annotated<FacePatchFeature, String>> negativeDataset2 )
		{
			this.classifier = new SVMAnnotator<FacePatchFeature,String>( new FaceFV() );
			this.negativeDataset = negativeDataset2;
		}

		/**
		 * 	Train the classifier with the positives and the negatives.
		 *
		 * 	@param person The person
		 *	@param positives The positive examples
		 *	@param negatives The negative examples
		 */
		public void train( final String person, final List<FacePatchFeature> positives )
		{
			// The dataset on which the classifier will be trained
			final List<Annotated<FacePatchFeature, String>> dataset
				= new ArrayList<Annotated<FacePatchFeature,String>>();

			System.out.println( "Retrieving positives..." );
			// The positive examples for the dataset
			for( final FacePatchFeature ffp : positives )
				dataset.add( AnnotatedObject.create( ffp, person ) );

			// Add the negatives
			dataset.addAll( this.negativeDataset );

			// Train the classifier
			try
			{
				this.classifier.train( dataset );
			}
			catch( final Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	private final ArrayList<Annotated<FacePatchFeature, String>> negativeDataset;

	/** List of all the person classifiers */
	public List<PersonClassifier> classifiers;

	/** Where to find the cached positive examples */
	public String positiveDir = "/home/dd/Dropbox/Work/cache/";

	/** The annotation to use for negative examples */
	private final String negativeClass = "unknown";

	public SpecificPersonRecogniser( final List<FacePatchFeature> negatives )
	{
		// The dataset on which the classifier will be trained
		this.negativeDataset = new ArrayList<Annotated<FacePatchFeature,String>>();

		System.out.println( "Retrieving negatives..." );
		// The negative examples for the dataset
		for( final FacePatchFeature ffp : negatives )
			this.negativeDataset.add( AnnotatedObject.create( ffp, this.negativeClass ) );
	}

	/**
	 * 	Trains the classifiers based on the positive examples in the positive
	 * 	directory, using the negatives in the negative directory in each of
	 * 	the classifiers.
	 *
	 *	@throws FileSystemException
	 */
	public void trainClassifiers() throws FileSystemException
	{
		// Get all the positive face patch features
		final VFSGroupDataset<FacePatchFeature> positives =
				new VFSGroupDataset<FacePatchFeature>(
						this.positiveDir, new FacePatchFeatureReader() );

		this.classifiers = new ArrayList<SpecificPersonRecogniser.PersonClassifier>();

		// Loop through all the groups found in the positive
		// and train a classifier for each one.
		for( final String group : positives.getGroups() )
		{
			System.out.println( "Training for "+group );

			// Create a new person classifier
			final PersonClassifier pc = new PersonClassifier( this.negativeDataset );

			final VFSListDataset<FacePatchFeature> g = positives.get( group );
			pc.train( group, g );

			this.classifiers.add( pc );
		}
	}

	/**
	 * 	Returns the face patch feature for the face in the image. If no face
	 * 	exists, then null is returned.
	 *	@param faceImage The face image
	 *	@return The face patch feature
	 */
	private FacePatchFeature getFacePatchFeature( final MBFImage faceImage )
	{
		// Detect faces
		final FKEFaceDetector fkd = new FKEFaceDetector();
		final List<KEDetectedFace> faces = fkd.detectFaces( faceImage.flatten() );

		// There should only be one.
		if( faces.size() > 0 )
		{
			// Choose the first face...
			System.out.println( "    - Found " + faces.size() + " faces." );
			final KEDetectedFace face = faces.get( 0 );

			// Extract the face patch
			final Extractor fpfe = new FacePatchFeature.Extractor();
			final FacePatchFeature fpf = fpfe.extractFeature( face );

			return fpf;
		}

		return null;
	}

	/**
	 * 	Given a face image will attempt to return an annotation for the person.
	 *	@param faceImage The face image.
	 *	@return The annotation or NULL
	 */
	public String annotate( final MBFImage faceImage )
	{
		final FacePatchFeature fpf = this.getFacePatchFeature( faceImage );

		// Loop through all the classifiers getting an annotation
		final double maxScore = 0;
		final String maxScoredAnnotation = null;
		for( final PersonClassifier pc : this.classifiers )
		{
			final List<ScoredAnnotation<String>> x = pc.classifier.annotate( fpf );
			System.out.println( x );
		}

		return null;
	}

	public double matchFaceImage( final MBFImage faceImage )
	{
		final double score = 0;

		final FacePatchFeature fpf = this.getFacePatchFeature( faceImage );

		// Loop through all the classifiers getting an annotation
		final double maxScore = 0;
		final String maxScoredAnnotation = null;
		for( final PersonClassifier pc : this.classifiers )
		{
			final List<ScoredAnnotation<String>> x = pc.classifier.annotate( fpf );
			System.out.println( x );
		}

		return score;
	}

	public static void main( final String[] args )
	{
		try
		{
			// Where to find the negative examples
			final String negativeDir = "/data/degas/openimaj/datasets/CaltechWebFaces/pictures/";

			// Get all the negative face patch features
			final VFSListDataset<FacePatchFeature> negatives =
				new VFSListDataset<FacePatchFeature>( negativeDir,
						new FacePatchFeatureReader() );

			final SpecificPersonRecogniser spr = new SpecificPersonRecogniser( negatives );

			System.out.println( "Training..." );
			spr.trainClassifiers();

			final MBFImage faceImage = ImageUtilities.readMBF(
				new File( "/home/dd/Dropbox/Work/mediaeval/faces/cache/" +
						"20080401_010000_bbcfour_the_book_quiz.webm/002829-002949-face.png") );
			final double x = spr.matchFaceImage( faceImage );
			System.out.println( "Final score: "+x );
		}
		catch( final FileSystemException e )
		{
			e.printStackTrace();
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}
	}
}
