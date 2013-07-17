package org.openimaj.mediaeval.evaluation.datasets;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;
import gov.sandia.cognition.math.matrix.mtj.SparseRowMatrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import no.uib.cipr.matrix.sparse.FlexCompColMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

import org.apache.log4j.Logger;
import org.openimaj.data.identity.Identifiable;
import org.openimaj.feature.FeatureVector;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.CFMatrixUtils;
import org.openimaj.mediaeval.data.Count;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.Head;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator.Mean;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.util.function.Function;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.Stream;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013SimilarityMatrix {

	private static Logger logger = Logger.getLogger(SED2013SimilarityMatrix.class);
	private static final class PhotoToIdentifiablePhoto implements Function<Photo, IdentifiablePhoto> {
		@Override
		public IdentifiablePhoto apply(Photo in) {
			return new IdentifiablePhoto(in);
		}
	}
	/**
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public static class IdentifiablePhoto implements Identifiable{
		protected Photo p;
		/**
		 * @param p
		 */
		public IdentifiablePhoto(Photo p) {
			this.p = p;
		}
		@Override
		public String getID() {
			return p.getId();
		}

	}
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
//		String expHome = "/home/ss/Experiments/mediaeval/SED2013";
//		String expName = "training.smalltest";
//		String expName = "training.all";
//		String bigFile = "/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml";
//		String bigFile = "/home/ss/Experiments/mediaeval/SED2013/sed2013_dataset_train.xml";
		
		if(args.length!=3){
			logger.error("Usage:<cmd> experiment_name experiment_home photo_xml_file ");
		}
		String expName = args[0];
		String expHome = args[1];
		String bigFile = args[2];
		
		
		String tfidfSource = String.format("%s/%s",expHome,"training.sed2013.photo_tfidf");
//		List<ExtractorComparator<Photo, DoubleFV>> excomps = SED2013ExpOne.PPK2012Similarity(tfidfSource);
		List<ExtractorComparator<Photo, ? extends FeatureVector>> excomps = PPK2012ExtractCompare.similarity(tfidfSource,new File(expHome,expName + ".featurecache").getAbsolutePath());
		final Mean<Photo> comp = new CombinedFVComparator.Mean<Photo>(excomps) ;
		logger .info(String.format("Loading dataset: %s ", bigFile));
		final File xmlFile = new File(bigFile);
		Count<CursorWrapper> count = new Count<CursorWrapper>();
		createStream(xmlFile)
		.forEach(count);
		
//		similarityMatrixFromFile(comp, xmlFile, sparse);
		similarityMatrixFromMemory(comp, xmlFile, (int) count.seen,new File(String.format("%s/%s.sparse",expHome,expName )));

	}
	private static void similarityMatrixFromMemory(
			final Mean<Photo> comp,
			final File xmlFile,
			int count,
			File sparseRowDir) throws XMLStreamException,
			ParseException, IOException
	{
		sparseRowDir.mkdirs();
		logger.error("Loading all photos into memory");
		final List<Photo> allPhotos = new ArrayList<Photo>();
		createStream(xmlFile)
		.map(new CursorWrapperPhoto())
		.forEach(new Operation<Photo>() {
			@Override
			public void perform(Photo object) {
				allPhotos.add(object);
			}

		});

		for (int i = 0; i < allPhotos.size(); i++) {
			SparseMatrix rmat = SparseMatrixFactoryMTJ.INSTANCE.copyMatrix(SparseMatrixFactoryMTJ.INSTANCE.createWrapper(new FlexCompRowMatrix(1, count)));
			for (int j = i; j < allPhotos.size(); j++) {
				double d = comp.compare(allPhotos.get(i), allPhotos.get(j));
				if(threshold(d)){
					rmat.setElement(0, j, d);
				}
				j++;
			}
			// flush the row matrix
			File rowout = new File(sparseRowDir,String.format("%d.mat",i));
			IOUtils.writeToFile(rmat.getInternalMatrix(), rowout);
			if(i%100 == 0){
				logger.debug(String.format("Similarity calculated for row %d, last sparcity: %2.5f",i,CFMatrixUtils.sparcity(rmat)));
			}
		}
	}
//	private static void similarityMatrixFromFile(
//			final Mean<Photo> comp,
//			final File xmlFile,
//			final SparseMatrix sparse) throws XMLStreamException,
//			FileNotFoundException, ParseException
//	{
//		createStream(xmlFile)
//		.map(new CursorWrapperPhoto())
//		.forEach(new Operation<Photo>() {
//			final int[] i = new int[]{0};
//			@Override
//			public void perform(final Photo iobj) {
//				try {
//					logger.error("Starting Row: " + i[0]);
//					createStream(xmlFile)
//					.filter(new SkipFilter<CursorWrapper>(i[0]))
//					.map(new CursorWrapperPhoto())
//					.forEach(new Operation<Photo>() {
//						int j = i[0];
//						@Override
//						public void perform(final Photo jobj) {
//							if(j%100 == 0){
//								logger.error("...Done Col: " + j);
//							}
//							double d = comp.compare(iobj, jobj);
//							if(threshold(d)){
//								sparse.setElement(i[0], j, d);
//								sparse.setElement(j, i[0], d);
//							}
//							j++;
//						}
//
//					});
//				} catch (Exception e){
//					logger.error("Couldn't parse file after seen: " + i[0],e);
//				}
//				i[0]++;
//			}
//		});
//	}
	private static boolean threshold(double d) {
		return d>0.1;
	}
	private static Stream<CursorWrapper> createStream(final File xmlFile) throws XMLStreamException,FileNotFoundException, ParseException
	{
		final SimpleDateFormat df = new SimpleDateFormat("yyyy MM");
		final Date after = df.parse("2007 01");
		final Date before = df.parse("2007 02");

		return new XMLCursorStream(xmlFile,"photo")
//		.filter(new CursorDateFilter(after, before))
//		.filter(new Head<CursorWrapper>(10000))
		;
	}
}
