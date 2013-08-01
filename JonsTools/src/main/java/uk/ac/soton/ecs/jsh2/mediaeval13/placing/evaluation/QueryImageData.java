package uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation;

import java.util.Date;

/**
 * A query image used as input to a {@link GeoPositioningEngine}.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class QueryImageData {
	private final static String CSV_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))";

	public long flickrId;
	public String tags;
	public String userId;
	public String url;
	public Date dateTaken;
	public Date dateUploaded;
	public int numberOfViews;

	public QueryImageData(long id) {
		this.flickrId = id;
	}

	public static QueryImageData parseCSVLine(String line) {
		if (line.startsWith("photo"))
			return null;

		final String[] parts = line.split(CSV_REGEX);

		// photoID,accuracy,userID,photoLink,photoTags,DateTaken,DateUploaded,views,licenseID
		final long flickrId = Long.parseLong(parts[0]);
		final QueryImageData data = new QueryImageData(flickrId);

		data.userId = parts[2];
		data.url = parts[3];
		data.tags = parts[4];
		data.dateTaken = new Date(Long.parseLong(parts[5]));
		data.dateUploaded = new Date(Long.parseLong(parts[6]));
		data.numberOfViews = Integer.parseInt(parts[7]);

		return data;
	}
}
