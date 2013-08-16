package org.openimaj.mediaeval.placement.search;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.semanticmetadata.lire.AbstractImageSearcher;
import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageDuplicates;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.impl.GenericDocumentBuilder;
import net.semanticmetadata.lire.impl.SimpleImageDuplicates;
import net.semanticmetadata.lire.impl.SimpleImageSearchHits;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.utils.ImageUtils;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class GenericFastImageSearcher extends AbstractImageSearcher {

    protected Logger logger = Logger.getLogger( getClass().getName() );
    Class<?> descriptorClass;
    String fieldName;
    private LireFeature cachedInstance = null;
    private boolean isCaching = false;

    private LinkedList<byte[]> featureCache;
    private IndexReader reader;

    private int maxHits = 10;
    protected TreeSet<SimpleResult> docs;
    private float maxDistance;

    public GenericFastImageSearcher( int maxHits, Class<?> descriptorClass, String fieldName ) {
        this.maxHits = maxHits;
        docs = new TreeSet<SimpleResult>();
        this.descriptorClass = descriptorClass;
        this.fieldName = fieldName;

        try {
            this.cachedInstance = (LireFeature) this.descriptorClass.newInstance();
        } catch( InstantiationException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher (" + descriptorClass.getName() + "): " + e.getMessage() );
        } catch( IllegalAccessException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher (" + descriptorClass.getName() + "): " + e.getMessage() );
        }

        init();
    }

    public GenericFastImageSearcher( int maxHits, Class<?> descriptorClass, String fieldName, boolean isCaching, IndexReader reader ) {
        this.isCaching = isCaching;
        this.maxHits = maxHits;
        docs = new TreeSet<SimpleResult>();
        this.descriptorClass = descriptorClass;
        this.fieldName = fieldName;

        try {
            this.cachedInstance = (LireFeature) this.descriptorClass.newInstance();
        } catch( InstantiationException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher (" + descriptorClass.getName() + "): " + e.getMessage() );
        } catch( IllegalAccessException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher (" + descriptorClass.getName() + "): " + e.getMessage() );
        }

        this.reader = reader;
        init();
    }

    private void init() {
        // Load all respective features into an in-memory cache
        if( isCaching && reader != null ) {
            int docs = reader.numDocs();
            featureCache = new LinkedList<byte[]>();
            try {
                Document d;
                for( int i = 0; i < docs; i++ ) {
                    d = reader.document( i );
                    cachedInstance.setByteArrayRepresentation( d.getField( fieldName ).binaryValue().bytes, d.getField( fieldName ).binaryValue().offset, d.getField( fieldName ).binaryValue().length );
                    featureCache.add( cachedInstance.getByteArrayRepresentation() );
                }
            } catch( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ImageSearchHits search( BufferedImage image, IndexReader reader ) throws IOException {
        logger.finer( "Starting extraction." );
        LireFeature lireFeature = null;
        SimpleImageSearchHits searchHits = null;

        try {
            lireFeature = (LireFeature) descriptorClass.newInstance();
            
            // Scale image
            BufferedImage bimg = image;
            if( Math.max( image.getHeight(), image.getWidth() ) > GenericDocumentBuilder.MAX_IMAGE_DIMENSION ) {
                bimg = ImageUtils.scaleImage( image,  GenericDocumentBuilder.MAX_IMAGE_DIMENSION );
            }
            lireFeature.extract(bimg);
            logger.fine( "Extraction from image finished" );
            
            float maxDistance = findSimilar( reader, lireFeature );
            searchHits = new SimpleImageSearchHits( this.docs, maxDistance );
        } catch( InstantiationException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher: " + e.getMessage() );
        } catch( IllegalAccessException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher: " + e.getMessage() );
        }
        
        return searchHits;
    }
    
    @Override
    public ImageSearchHits search( Document doc, IndexReader reader ) throws IOException {
        SimpleImageSearchHits searchHits = null;
        
        try {
            LireFeature lireFeature = (LireFeature) descriptorClass.newInstance();
            if( doc.getField( fieldName ).binaryValue() != null && doc.getField( fieldName ).binaryValue().length > 0 )
                lireFeature.setByteArrayRepresentation( doc.getField( fieldName ).binaryValue().bytes, doc.getField( fieldName ).binaryValue().offset, doc.getField( fieldName ).binaryValue().length );
            
            float maxDistance = findSimilar( reader, lireFeature );
            
            searchHits = new SimpleImageSearchHits( this.docs, maxDistance );
        } catch( InstantiationException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher: " + e.getMessage() );
        } catch( IllegalAccessException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher: " + e.getMessage() );
        }
        
        return searchHits;
    }
    
    @Override
    public ImageDuplicates findDuplicates( IndexReader reader ) throws IOException {
        SimpleImageDuplicates simpleImageDuplicates = null;
        
        try {
            // Get the first document
            Document doc = reader.document( 0 );
            
            LireFeature lireFeature = (LireFeature) descriptorClass.newInstance();
            if( doc.getField( fieldName ).binaryValue() != null && doc.getField( fieldName ).binaryValue().length > 0 )
                lireFeature.setByteArrayRepresentation( doc.getField( fieldName ).binaryValue().bytes, doc.getField( fieldName ).binaryValue().offset, doc.getField( fieldName ).binaryValue().length );
            
            HashMap<Float, List<String>> duplicates = new HashMap<Float, List<String>>();
            
            // Needed to check whether the document is deleted
            Bits liveDocs = MultiFields.getLiveDocs( reader );
            
            int docs = reader.numDocs();
            int numDuplicates = 0;
            
            for( int i = 0; i < docs; i++ ) {
                // If document is deleted, ignore it
                if( reader.hasDeletions() && !liveDocs.get( i ) )
                    continue;
                
                Document d = reader.document( i );
                float distance = getDistance( d, lireFeature );
                
                if( !duplicates.containsKey( distance ) ) {
                    duplicates.put( distance, new LinkedList<String>() );
                }
                else {
                    numDuplicates++;
                }

                duplicates.get( distance ).add( d.getField( DocumentBuilder.FIELD_NAME_IDENTIFIER).stringValue() );
            }
            
            if( numDuplicates == 0 )
                return null;
            
            LinkedList<List<String>> results = new LinkedList<List<String>>();
            
            for( float f : duplicates.keySet() ) {
                if( duplicates.get( f ).size() > 1 ) {
                    results.add( duplicates.get( f ) );
                }
            }
            
            simpleImageDuplicates = new SimpleImageDuplicates( results );
        }
        catch( InstantiationException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher: " + e.getMessage() );
        } catch( IllegalAccessException e ) {
            logger.log( Level.SEVERE, "Error instantiating class for generic image searcher: " + e.getMessage() );
        }

        return simpleImageDuplicates;
    }

    protected float findSimilar( IndexReader reader, LireFeature lireFeature ) throws IOException {
        maxDistance = -1f;
        
        // Clear the result set
        docs.clear();
        
        // Check whether the document is deleted
        Bits liveDocs = MultiFields.getLiveDocs( reader );
        Document d;
        float tmpDistance;
        int docs = reader.numDocs();
        
        if( !isCaching ) {
            // Read each and every document from the index and then compare it to the query
            for( int i = 0; i < docs; i++ ) {
                
                // If document is deleted, ignore it
                if( reader.hasDeletions() && !liveDocs.get( i ) )
                    continue;
                
                d = reader.document( i );
                tmpDistance = getDistance( d, lireFeature );
                
                // If the distance is invalid, ignore it
                if( tmpDistance < 0 )
                    continue;
                
                // If this document is the first document
                if( maxDistance < 0 ) {
                    maxDistance = tmpDistance;
                }
                
                // If the array is not full yet
                if( this.docs.size() < maxHits ) {
                    this.docs.add( new SimpleResult( tmpDistance, d, i ) );
                    if( tmpDistance > maxDistance )
                        maxDistance = tmpDistance;
                }
                // If this document is nearer to the query than at least one of the current set
                else if( tmpDistance < maxDistance ) {
                    // Remove the last one
                    this.docs.remove( this.docs.last() );
                    
                    // Add the new one
                    this.docs.add( new SimpleResult( tmpDistance, d, i ) );
                    
                    // And set the new distance border
                    maxDistance = this.docs.last().getDistance();
                }
            }
        }
        else {
            // Use the in-memory cache to find the matching docs from the index
            int count = 0;
            for( Iterator<byte[]> iterator = featureCache.iterator(); iterator.hasNext(); ) {
                cachedInstance.setByteArrayRepresentation( iterator.next() );
                
                // If the document is deleted, ignore it
                if( reader.hasDeletions() && !liveDocs.get( count ) ) {
                    count++;
                    continue;
                }
                else {
                    tmpDistance = lireFeature.getDistance( cachedInstance );
                    
                    // If the distance is invalid, ignore it
                    if( tmpDistance < 0 )
                        continue;
                    
                    // If it is the first document
                    if( maxDistance < 0 )
                        maxDistance = tmpDistance;
                    
                    // If the array is not full yet
                    if( this.docs.size() < maxHits ) {
                        this.docs.add( new SimpleResult( tmpDistance, reader.document( count ), count ) );
                        if( tmpDistance > maxDistance )
                            maxDistance = tmpDistance;
                    }
                    // If this document is nearer to the query than at least one of the current set
                    else if( tmpDistance < maxDistance ) {
                        // Remote the last one
                        this.docs.remove( this.docs.last() );
                        
                        // Add the new one
                        this.docs.add( new SimpleResult( tmpDistance, reader.document( count ), count ) );
                        
                        // Set the new distance border
                        maxDistance = this.docs.last().getDistance();
                    }
                    count++;
                }
            }
        }

        return maxDistance;
    }
    
    protected float getDistance( Document document, LireFeature lireFeature ) {
        if( document.getBinaryValue( fieldName ) != null && document.getBinaryValue( fieldName ).length > 0 ) {
            cachedInstance.setByteArrayRepresentation( document.getBinaryValue( fieldName ).bytes, document.getBinaryValue( fieldName ).offset, document.getBinaryValue( fieldName ).length );
            return lireFeature.getDistance( cachedInstance );
        } else {
            logger.warning( "No " + descriptorClass.getName() + " feature stored for " + document.get( LuceneIndexBuilder.FIELD_ID ) );
        }
        
        return -1f;
    }

    public String toString() {
        return "GenericFastImageSearcher using " + descriptorClass.getName();
    }
}