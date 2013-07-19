package org.openimaj.mediaeval.evaluation.datasets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.Head;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.mediaeval.feature.extractor.PhotoDescription;
import org.openimaj.mediaeval.feature.extractor.PhotoTags;
import org.openimaj.mediaeval.feature.extractor.PhotoTitle;
import org.openimaj.mediaeval.feature.extractor.TFIDF;
import org.openimaj.util.function.Operation;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.stream.Stream;

import com.aetrion.flickr.photos.Photo;

/**
 * Train and save the {@link TFIDF} instances agains a {@link Stream} of {@link Photo} instances
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013TFIDFTrainer {
	private static Logger logger = Logger.getLogger(SED2013TFIDFTrainer.class);

	/**
	 * @param source
	 * @param totrain
	 */
	public SED2013TFIDFTrainer(Stream<Photo> source, final List<TFIDF<Photo>> totrain) {
		source.forEach(new Operation<Photo>() {
			@Override
			public void perform(Photo object) {
				for (TFIDF<Photo> tfidf : totrain) {
					tfidf.update(object);
				}
			}
		});
	}

	public static void main(String[] args) throws XMLStreamException, IOException {
//		String bigFile = "/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml";
		String bigFile = "/home/ss/Experiments/mediaeval/SED2013/sed2013_dataset_train.xml";
		logger .info(String.format("Loading dataset: %s ", bigFile));
		File xmlFile = new File(bigFile);

		Stream<Photo> photoStream = new XMLCursorStream(xmlFile,"photo")
		.map(new CursorWrapperPhoto())
		;
		List<TFIDF<Photo>> tfidfList ;
//		String expHome = "/Users/ss/Experiments/sed2013";
		String expHome = "/home/ss/Experiments/mediaeval/SED2013";
		String tfidfSource = String.format("%s/%s",expHome,"training.sed2013.photo_tfidf");
//		String tfidfSource = String.format("%s/%s",expHome,"test.photo_tfidf");
		tfidfList = new ArrayList<TFIDF<Photo>>();
		tfidfList.add(new TFIDF<Photo>(new PhotoTags(true)));
		tfidfList.add(new TFIDF<Photo>(new PhotoDescription()));
		tfidfList.add(new TFIDF<Photo>(new PhotoTitle()));
		new SED2013TFIDFTrainer(photoStream, tfidfList);
		IOUtils.writeToFile(tfidfList, new File(tfidfSource));
		System.out.println(tfidfList.size());
		System.out.println(tfidfList.get(0).getDF().size());
		tfidfList = IOUtils.readFromFile(new File(tfidfSource));
		System.out.println(tfidfList.size());
		TFIDF<Photo> tagTFIDF = tfidfList.get(0);
		System.out.println(tagTFIDF.getDF().size());
//		int count = 0;
//		double maxCount = 0;
//		String maxWord = null;
//		for (Entry<String, Double> df : tagTFIDF.getDF().entrySet()) {
//			if(df.getValue() < 5) {
//				System.out.println(df.getKey());
//				count ++;
//			}
//			if(maxCount < df.getValue()){
//				maxWord = df.getKey();
//			}
//			maxCount = Math.max(maxCount,df.getValue());
//		}
//		System.out.println("Small DF count: " + count);
//		System.out.println("Max DF count: " + maxCount + " belonging to: " + maxWord);
	}


}
