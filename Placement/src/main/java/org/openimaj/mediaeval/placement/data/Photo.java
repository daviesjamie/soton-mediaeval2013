package org.openimaj.mediaeval.placement.data;

import java.util.ArrayList;
import java.util.Date;

import org.openimaj.mediaeval.placement.utils.MongoUtils;

import com.mongodb.DBObject;

/**
 * A representation of a single photo from the Placement task dataset.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class Photo {

    /** The photo metadata */
    private String photoID;
    private byte accuracy;
    private String userID;
    private String photoLink;
    private String photoTags;
    private Date dateTaken;
    private Date dateUploaded;
    private int views;
    private byte licenseID;
    private double latitude;
    private double longitude;

    /**
     * Constructs a {@link Photo} object from a MongoDB {@link DBObject}.
     * 
     * @param object
     *            The {@link DBObject} containing the data to construct a
     *            {@link Photo} from.
     * @return A {@link Photo} object containing the data from the supplied
     *         {@link object}.
     */
    public static Photo parsePhoto( DBObject object ) {
        Photo p = new Photo();
        
        if( object.get( "photoID" ) != null )
            p.setPhotoID( object.get( "photoID" ).toString() );
        if( object.get( "accuracy" ) != null )
            p.setAccuracy( Byte.valueOf( object.get( "accuracy" ).toString() ) );
        if( object.get( "userID" ) != null )
            p.setUserID( object.get( "userID" ).toString() );
        if( object.get( "photoLink" ) != null )
            p.setPhotoLink( object.get( "photoLink" ).toString() );
        if( object.get( "photoTags" ) != null )
            p.setPhotoTags( object.get( "photoTags" ).toString() );
        if( object.get( "DateTaken" ) != null )
            p.setDateTaken( new Date( Long.valueOf( object.get( "DateTaken" ).toString() ) ) );
        if( object.get( "DateUploaded" ) != null )
            p.setDateUploaded( new Date( Long.valueOf( object.get( "DateUploaded" ).toString() ) ) );
        if( object.get( "views" ) != null )
            p.setViews( Integer.valueOf( object.get( "views" ).toString() ) );
        if( object.get( "licenseID" ) != null )
            p.setLicenseID( Byte.valueOf( object.get( "licenseID" ).toString() ) );
        
        if( object.get( "location" ) != null ) {
            ArrayList<Double> coords = MongoUtils.extractLatLong( object );
            p.setLatitude( coords.get( 1 ) );
            p.setLongitude( coords.get( 0 ) );
        }

        return p;
    }

    public String getPhotoID() {
        return photoID;
    }

    public void setPhotoID( String photoID ) {
        this.photoID = photoID;
    }

    public byte getAccuracy() {
        return accuracy;
    }

    public void setAccuracy( byte accuracy ) {
        this.accuracy = accuracy;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID( String userID ) {
        this.userID = userID;
    }

    public String getPhotoLink() {
        return photoLink;
    }

    public void setPhotoLink( String photoLink ) {
        this.photoLink = photoLink;
    }

    public String getPhotoTags() {
        return photoTags;
    }

    public void setPhotoTags( String photoTags ) {
        this.photoTags = photoTags;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken( Date dateTaken ) {
        this.dateTaken = dateTaken;
    }

    public Date getDateUploaded() {
        return dateUploaded;
    }

    public void setDateUploaded( Date dateUploaded ) {
        this.dateUploaded = dateUploaded;
    }

    public int getViews() {
        return views;
    }

    public void setViews( int views ) {
        this.views = views;
    }

    public byte getLicenseID() {
        return licenseID;
    }

    public void setLicenseID( byte licenseID ) {
        this.licenseID = licenseID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude( double latitude ) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude( double longitude ) {
        this.longitude = longitude;
    }

}
