package org.openimaj.mediaeval.data.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.Editability;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Note;
import com.aetrion.flickr.photos.Permissions;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotoUrl;
import com.aetrion.flickr.photos.Size;
import com.aetrion.flickr.tags.Tag;
import com.aetrion.flickr.util.XMLUtilities;

/**
 * Utilitiy-methods to transfer requested XML to Photo-objects.
 *
 * @author till, x-mago
 * @version $Id: PhotoUtils.java,v 1.20 2009/07/23 21:49:35 x-mago Exp $
 */
public final class PhotoUtils {
	private static final long serialVersionUID = 12L;
	public static final ThreadLocal DATE_FORMATS = new ThreadLocal() {
        @Override
		protected synchronized Object initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };
    private PhotoUtils() {
    }

    /**
     * Try to get an attribute value from two elements.
     *
     * @param firstElement
     * @param secondElement
     * @return attribute value
     */
    private static String getAttribute(String name, Element firstElement,
            Element secondElement) {
        String val = firstElement.getAttribute(name);
        if (val.length() == 0 && secondElement != null) {
            val = secondElement.getAttribute(name);
        }
        return val;
    }

    /**
     * Transfer the Information of a photo from a DOM-object
     * to a Photo-object.
     *
     * @param photoElement
     * @return Photo
     */
    public static final Photo createPhoto(Element photoElement) {
        return createPhoto(photoElement, null);
    }

    /**
     * Transfer the Information of a photo from a DOM-object
     * to a Photo-object.
     *
     * @param photoElement
     * @param defaultElement
     * @return Photo
     */
     public static final Photo createPhoto(Element photoElement,
        Element defaultElement) {
        Photo photo = new Photo();
        photo.setId(photoElement.getAttribute("id"));
        photo.setPlaceId(photoElement.getAttribute("place_id"));
        photo.setSecret(photoElement.getAttribute("secret"));
        photo.setServer(photoElement.getAttribute("server"));
        photo.setFarm(photoElement.getAttribute("farm"));
        photo.setRotation(photoElement.getAttribute("rotation"));
        photo.setFavorite("1".equals(photoElement.getAttribute("isfavorite")));
        photo.setLicense(photoElement.getAttribute("license"));
        photo.setOriginalFormat(photoElement.getAttribute("originalformat"));
        photo.setOriginalSecret(photoElement.getAttribute("originalsecret"));
        photo.setIconServer(photoElement.getAttribute("iconserver"));
        photo.setIconFarm(photoElement.getAttribute("iconfarm"));
        String taken = getAttrAlternatives(photoElement,"datetaken","dateTaken");
		photo.setDateTaken(taken);
        String uploaded = getAttrAlternatives(photoElement,"dateuploaded","dateupload","dateUploaded","dateUpload");
        try{
        	Long.parseLong(uploaded);
        	photo.setDatePosted(uploaded);
        }catch(Exception e){
        	try {
        		photo.setDatePosted(((DateFormat)DATE_FORMATS.get()).parse(uploaded));
			} catch (ParseException e1) {

			}
        }
        photo.setLastUpdate(photoElement.getAttribute("lastupdate"));
        // flickr.groups.pools.getPhotos provides this value!
        String added = photoElement.getAttribute("dateadded");
		photo.setDateAdded(added);
        photo.setOriginalWidth(photoElement.getAttribute("o_width"));
        photo.setOriginalHeight(photoElement.getAttribute("o_height"));
        photo.setMedia(photoElement.getAttribute("media"));
        photo.setMediaStatus(photoElement.getAttribute("media_status"));
        photo.setPathAlias(photoElement.getAttribute("pathalias"));

        // If the attributes active that contain the image-urls,
        // Size-objects created from them, which are used to override
        // the Url-generation.
        List sizes = new ArrayList();
        String urlTmp = photoElement.getAttribute("url_t");
        if (urlTmp.startsWith("http")) {
            Size sizeT = new Size();
            sizeT.setLabel(Size.THUMB);
            sizeT.setSource(urlTmp);
            sizes.add(sizeT);
        }
        urlTmp = photoElement.getAttribute("url_s");
        if (urlTmp.startsWith("http")) {
            Size sizeT = new Size();
            sizeT.setLabel(Size.SMALL);
            sizeT.setSource(urlTmp);
            sizes.add(sizeT);
        }
        urlTmp = photoElement.getAttribute("url_sq");
        if (urlTmp.startsWith("http")) {
            Size sizeT = new Size();
            sizeT.setLabel(Size.SQUARE);
            sizeT.setSource(urlTmp);
            sizes.add(sizeT);
        }
        urlTmp = photoElement.getAttribute("url_m");
        if (urlTmp.startsWith("http")) {
            Size sizeT = new Size();
            sizeT.setLabel(Size.MEDIUM);
            sizeT.setSource(urlTmp);
            sizes.add(sizeT);
        }
        urlTmp = photoElement.getAttribute("url_l");
        if (urlTmp.startsWith("http")) {
            Size sizeT = new Size();
            sizeT.setLabel(Size.LARGE);
            sizeT.setSource(urlTmp);
            sizes.add(sizeT);
        }
        urlTmp = photoElement.getAttribute("url_o");
        if (urlTmp.startsWith("http")) {
            Size sizeT = new Size();
            sizeT.setLabel(Size.ORIGINAL);
            sizeT.setSource(urlTmp);
            sizes.add(sizeT);
        }
        if (sizes.size() > 0) {
            photo.setSizes(sizes);
        }

        // Searches, or other list may contain orginal_format.
        // If not choosen via extras, set jpg as default.
        try {
            if (photo.getOriginalFormat().equals("")) {
                photo.setOriginalFormat("jpg");
            }
        } catch (NullPointerException e) {
            photo.setOriginalFormat("jpg");
        }

        try {
            Element ownerElement = (Element) photoElement.getElementsByTagName("owner").item(0);
            if (ownerElement == null) {
                User owner = new User();
                owner.setId(getAttribute("owner", photoElement, defaultElement));

                owner.setUsername(getAttrAlternatives(photoElement,"username","ownername"));
                photo.setOwner(owner);
                photo.setUrl("http://flickr.com/photos/" + owner.getId() + "/" + photo.getId());
            } else {
                User owner = new User();
                owner.setId(ownerElement.getAttribute("nsid"));

                String username = ownerElement.getAttribute("username");
                String ownername = ownerElement.getAttribute("ownername");
                // try to get the username either from the "username" attribute or
                // from the "ownername" attribute
                if (username != null && !"".equals(username)) {
                    owner.setUsername(username);
                } else if (ownername != null && !"".equals(ownername)) {
                    owner.setUsername(ownername);
                }

                owner.setUsername(ownerElement.getAttribute("username"));
                owner.setRealName(ownerElement.getAttribute("realname"));
                owner.setLocation(ownerElement.getAttribute("location"));
                photo.setOwner(owner);
                photo.setUrl("http://flickr.com/photos/" + owner.getId() + "/" + photo.getId());
            }
        } catch (IndexOutOfBoundsException e) {
            User owner = new User();
            owner.setId(photoElement.getAttribute("owner"));
            owner.setUsername(photoElement.getAttribute("ownername"));
            photo.setOwner(owner);
            photo.setUrl("http://flickr.com/photos/" + owner.getId() + "/" + photo.getId());
        }

        try {
            photo.setTitle(XMLUtilities.getChildValue(photoElement, "title"));
            if (photo.getTitle() == null) {
                photo.setTitle(photoElement.getAttribute("title"));
            }
        } catch (IndexOutOfBoundsException e) {
            photo.setTitle(photoElement.getAttribute("title"));
        }

        try {
            photo.setDescription(XMLUtilities.getChildValue(photoElement, "description"));
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            // here the flags are set, if the photo is read by getInfo().
            Element visibilityElement = (Element) photoElement.getElementsByTagName("visibility").item(0);
            photo.setPublicFlag("1".equals(visibilityElement.getAttribute("ispublic")));
            photo.setFriendFlag("1".equals(visibilityElement.getAttribute("isfriend")));
            photo.setFamilyFlag("1".equals(visibilityElement.getAttribute("isfamily")));
        } catch (IndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
            // these flags are set here, if photos read from a list.
            photo.setPublicFlag("1".equals(photoElement.getAttribute("ispublic")));
            photo.setFriendFlag("1".equals(photoElement.getAttribute("isfriend")));
            photo.setFamilyFlag("1".equals(photoElement.getAttribute("isfamily")));
        }

        // Parse either photo by getInfo, or from list
        try {
            Element datesElement = XMLUtilities.getChild(photoElement, "dates");
            photo.setDatePosted(datesElement.getAttribute("posted"));
            photo.setDateTaken(datesElement.getAttribute("taken"));
            photo.setTakenGranularity(datesElement.getAttribute("takengranularity"));
            photo.setLastUpdate(datesElement.getAttribute("lastupdate"));
        } catch (IndexOutOfBoundsException e) {
            photo.setDateTaken(taken);
        } catch (NullPointerException e) {
            photo.setDateTaken(taken);
        }

        NodeList permissionsNodes = photoElement.getElementsByTagName("permissions");
        if (permissionsNodes.getLength() > 0) {
            Element permissionsElement = (Element) permissionsNodes.item(0);
            Permissions permissions = new Permissions();
            permissions.setComment(permissionsElement.getAttribute("permcomment"));
            permissions.setAddmeta(permissionsElement.getAttribute("permaddmeta"));
        }

        try {
            Element editabilityElement = (Element) photoElement.getElementsByTagName("editability").item(0);
            Editability editability = new Editability();
            editability.setComment("1".equals(editabilityElement.getAttribute("cancomment")));
            editability.setAddmeta("1".equals(editabilityElement.getAttribute("canaddmeta")));
            photo.setEditability(editability);
        } catch (IndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
            // nop
        }

        try {
            Element commentsElement = (Element) photoElement.getElementsByTagName("comments").item(0);
            photo.setComments(((Text) commentsElement.getFirstChild()).getData());
        } catch (IndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
            // nop
        }

        try {
            Element notesElement = (Element) photoElement.getElementsByTagName("notes").item(0);
            List notes = new ArrayList();
            NodeList noteNodes = notesElement.getElementsByTagName("note");
            for (int i = 0; i < noteNodes.getLength(); i++) {
                Element noteElement = (Element) noteNodes.item(i);
                Note note = new Note();
                note.setId(noteElement.getAttribute("id"));
                note.setAuthor(noteElement.getAttribute("author"));
                note.setAuthorName(noteElement.getAttribute("authorname"));
                note.setBounds(noteElement.getAttribute("x"), noteElement.getAttribute("y"),
                    noteElement.getAttribute("w"), noteElement.getAttribute("h"));
                note.setText(noteElement.getTextContent());
                notes.add(note);
            }
            photo.setNotes(notes);
        } catch (IndexOutOfBoundsException e) {
            photo.setNotes(new ArrayList());
        } catch (NullPointerException e) {
            photo.setNotes(new ArrayList());
        }

        // Tags coming as space-seperated attribute calling
        // InterestingnessInterface#getList().
        // Through PhotoInterface#getInfo() the Photo has a list of
        // Elements.
        try {
            List tags = new ArrayList();
            String tagsAttr = photoElement.getAttribute("tags");
            if (!tagsAttr.equals("")) {
                String[] values = tagsAttr.split("\\s+");
                for (int i = 0; i < values.length; i++) {
                    Tag tag = new Tag();
                    tag.setValue(values[i]);
                    tags.add(tag);
                }
            } else {
                 try {
                    Element tagsElement = (Element) photoElement.getElementsByTagName("tags").item(0);
                    NodeList tagNodes = tagsElement.getElementsByTagName("tag");
                    for (int i = 0; i < tagNodes.getLength(); i++) {
                        Element tagElement = (Element) tagNodes.item(i);
                        Tag tag = new Tag();
                        tag.setId(tagElement.getAttribute("id"));
                        tag.setAuthor(tagElement.getAttribute("author"));
                        tag.setRaw(tagElement.getAttribute("raw"));
                        tag.setValue(((Text) tagElement.getFirstChild()).getData());
                        tags.add(tag);
                    }
                } catch (IndexOutOfBoundsException e) {
                }
            }
            photo.setTags(tags);
        } catch (NullPointerException e) {
            photo.setTags(new ArrayList());
        }

        try {
            Element urlsElement = (Element) photoElement.getElementsByTagName("urls").item(0);
            List urls = new ArrayList();
            NodeList urlNodes = urlsElement.getElementsByTagName("url");
            for (int i = 0; i < urlNodes.getLength(); i++) {
                Element urlElement = (Element) urlNodes.item(i);
                PhotoUrl photoUrl = new PhotoUrl();
                photoUrl.setType(urlElement.getAttribute("type"));
                photoUrl.setUrl(XMLUtilities.getValue(urlElement));
                if (photoUrl.getType().equals("photopage")) {
                    photo.setUrl(photoUrl.getUrl());
                }
            }
            photo.setUrls(urls);
        } catch (IndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
            photo.setUrls(new ArrayList());
        }

        String longitude = null;
        String latitude = null;
        String accuracy = null;
        try {
            Element geoElement = (Element) photoElement.getElementsByTagName("location").item(0);
            longitude = geoElement.getAttribute("longitude");
            latitude = geoElement.getAttribute("latitude");
            accuracy = geoElement.getAttribute("accuracy");
        } catch (IndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
        	// Geodata may be available as attributes in the photo-tag itself!
            try {
                longitude = photoElement.getAttribute("longitude");
                latitude = photoElement.getAttribute("latitude");
                accuracy = photoElement.getAttribute("accuracy");
            } catch (NullPointerException e2) {
                // no geodata at all
            }
        }
        if (longitude != null && latitude != null) {
            if (longitude.length() > 0 && latitude.length() > 0
                && !("0".equals(longitude) && "0".equals(latitude))) {
            	if(accuracy.length()==0)accuracy = "" + Flickr.ACCURACY_STREET;
                photo.setGeoData(new GeoData(longitude, latitude, accuracy));
            }
        }

        photo.setUrl(photoElement.getAttribute("photo_url"));
        return photo;
    }

    private static String getAttrAlternatives(Element elm,String ... strings) {
		for (String string : strings) {
			String ret = elm.getAttribute(string);
			if(ret != null && !ret.equals("")) return ret;
		}
		return "";
	}

	/**
     * Parse a list of Photos from given Element.
     *
     * @param photosElement
     * @return PhotoList
     */
    public static final PhotoList createPhotoList(Element photosElement) {
        PhotoList photos = new PhotoList();
        photos.setPage(photosElement.getAttribute("page"));
        photos.setPages(photosElement.getAttribute("pages"));
        photos.setPerPage(photosElement.getAttribute("perpage"));
        photos.setTotal(photosElement.getAttribute("total"));

        NodeList photoNodes = photosElement.getElementsByTagName("photo");
        for (int i = 0; i < photoNodes.getLength(); i++) {
            Element photoElement = (Element) photoNodes.item(i);
            photos.add(PhotoUtils.createPhoto(photoElement));
        }
        return photos;
    }
    /**
     *
     * <photo id="85556119"
     * 	photo_url="http://farm1.staticflickr.com/39/85556119_4958c870fd.jpg"
     * 	username="niallkennedy"
     * 	dateTaken="2006-01-11 20:48:24.0"
     * 	dateUploaded="2006-01-12 09:05:13.0">
     * 		<title>Group shot</title>
     * 		<description>A shot of almost the entire crowd at the Mac small business dinner</description>
     * 		<tags>
     * 			<tag>macsb</tag>
     * 			<tag>macworld</tag>
     * 		</tags>
     * 		<location latitude="37.7836" longitude="-122.399"></location>
     * 	</photo>
     * @param p
     * @param elm
     * @return
     */
	public static Node createElement(Photo p, Document doc) {
		Element elm = doc.createElement("photo");
		elm.setAttribute("id", sanitize(p.getId()));
		elm.setAttribute("photo_url", sanitize(p.getUrl()));
		elm.setAttribute("username", sanitize(p.getOwner().getUsername()));
		elm.setAttribute("dateTaken", sanitize(((DateFormat)DATE_FORMATS.get()).format(p.getDateTaken())));
		elm.setAttribute("dateUploaded", sanitize(((DateFormat)DATE_FORMATS.get()).format(p.getDatePosted())));
		Element title = doc.createElement("title");
		title.appendChild(doc.createTextNode(sanitize(p.getTitle())));
		elm.appendChild(title);
		Element desc = doc.createElement("description");
		desc.appendChild(doc.createTextNode(sanitize(p.getDescription())));
		elm.appendChild(desc);
		Element tags = doc.createElement("tags");
		for (Object tagO : p.getTags()) {
			Element tag = doc.createElement("tag");
			Text tagContent = doc.createTextNode(sanitize(((Tag)tagO).getValue()));
			tag.appendChild(tagContent);
			tags.appendChild(tag);
		}
		elm.appendChild(tags);
		if(p.getGeoData()!=null){
			Element location = doc.createElement("location");
			location.setAttribute("latitude", "" + p.getGeoData().getLatitude());
			location.setAttribute("longitude", "" + p.getGeoData().getLongitude());
			elm.appendChild(location);
		}
		return elm;
	}

	private static String sanitize(String s) {
		if(s == null) return "";
		return s;
	}

	/**
	 * @param p
	 * @return
	 */
	public static String toString(Photo p) {
		String[] tags = new String[(p.getTags().size())];
		int i = 0;
		for (Object o : p.getTags()) {
			Tag t = (Tag)o;
			tags[i++] = t.getValue();
		}
		Date datePosted = p.getDatePosted();

		String upload = "";
		if(datePosted!=null)
			upload = ((SimpleDateFormat)DATE_FORMATS.get()).format(datePosted);
		Date dateTaken = p.getDateTaken();
		String taken = "";
		if(dateTaken!=null)
			taken = ((SimpleDateFormat)DATE_FORMATS.get()).format(dateTaken);
		GeoData geo = p.getGeoData();
		String geos = "None";
		if(geo!=null){
			geos = String.format("%2.5f,%2.5f",geo.getLatitude(),geo.getLongitude());
		}
		return String.format("tags=%s, upload=%s, taken=%s, geo=%s",Arrays.toString(tags),upload,taken,geos);
	}

}