/**
 * 
 */
package org.openimaj.mediaeval.searchhyper2013;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.FloatFV;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.face.feature.FacePatchFeature;
import org.openimaj.io.InputStreamObjectReader;
import org.openimaj.io.ObjectReader;
import org.openimaj.ml.annotation.Annotated;
import org.openimaj.ml.annotation.AnnotatedObject;
import org.openimaj.ml.annotation.svm.SVMAnnotator;

/**
 *	
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
		public FacePatchFeature read( InputStream source ) throws IOException
		{
			FacePatchFeature fpf = new FacePatchFeature();
			fpf.readBinary( new DataInputStream( source ) );
			return fpf;
		}

		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.io.ObjectReader#canRead(java.lang.Object, java.lang.String)
		 */
		@Override
		public boolean canRead( InputStream source, String name )
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
		public FloatFV extractFeature( FacePatchFeature object )
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
		
		/** The annotation to use for negative examples */
		private String negativeClass = "unknown";

		/**
		 * 	Construct a person classifier
		 */
		public PersonClassifier()
		{
			classifier = new SVMAnnotator<FacePatchFeature,String>( new FaceFV() );
		}
		
		/**
		 * 	Train the classifier with the positives and the negatives.
		 * 
		 * 	@param person The person
		 *	@param positives The positive examples
		 *	@param negatives The negative examples
		 */
		public void train( String person, List<FacePatchFeature> positives,
				List<FacePatchFeature> negatives )
		{
			List<Annotated<FacePatchFeature, String>> dataset
				= new ArrayList<Annotated<FacePatchFeature,String>>();
			
			for( FacePatchFeature ffp : positives )
				dataset.add( AnnotatedObject.create( ffp, person ) );
			
			for( FacePatchFeature ffp : negatives )
				dataset.add( AnnotatedObject.create( ffp, negativeClass ) );
			
			classifier.train( dataset );
		}
	}
	
	/** List of all the person classifiers */
	public List<PersonClassifier> classifiers;
	
	/** Where to find the cached positive examples */
	public String positiveDir = "/home/dd/Dropbox/Work/cache/";
	
	/** Where to find the negative examples */
	public String negativeDir = "/data/degas/openimaj/datasets/CaltechWebFaces/pictures/";
	
	/**
	 * 	Trains the classifiers based on the positive examples in the positive
	 * 	directory, using the negatives in the negative directory in each of
	 * 	the classifiers.
	 * 
	 *	@throws FileSystemException
	 */
	public void trainClassifiers() throws FileSystemException
	{
		// Get all the negative face patch features
		VFSListDataset<FacePatchFeature> negatives = 
			new VFSListDataset<FacePatchFeature>( negativeDir, 
					new FacePatchFeatureReader() );
		
		// Get all the positive face patch features
		VFSGroupDataset<FacePatchFeature> positives = 
				new VFSGroupDataset<FacePatchFeature>( 
						positiveDir, new FacePatchFeatureReader() );
		
		// Loop through all the groups found in the positive 
		// and train a classifier for each one.
		for( String group : positives.getGroups() )
		{
			// Create a new person classifier
			PersonClassifier pc = new PersonClassifier();
			
			VFSListDataset<FacePatchFeature> g = positives.get( group );
			pc.train( group, g, negatives );
		}
	}
	
	public double matchFaceImage( MBFImage faceImage )
	{
		double score = 0;
		return score;
	}
}
