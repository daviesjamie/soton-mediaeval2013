package org.openimaj.mediaeval.utils;

import jal.objects.Sorting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne.Training;
import org.openimaj.util.queue.BoundedPriorityQueue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import clojure.main;

import com.Ostermiller.util.CSVPrinter;
import com.aetrion.flickr.photos.Photo;

/**
 * Using SED2013 {@link Training}, construct a small subset in the same input format
 * containing some smaller number of items in a reasonable number of clusters which
 * are chronologically close in time 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013SubsetGenerator {
	private Training training;
	private Logger logger = Logger.getLogger(SED2013SubsetGenerator.class);

	/**
	 * @param clusterCSV
	 * @param photosXML 
	 * @param string2 
	 * @param string 
	 * @throws IOException 
	 */
	public SED2013SubsetGenerator(InputStream clusterCSV, InputStream photosXML, String clusterOut, String photoOut) throws IOException {
		this.training = new Training(clusterCSV, photosXML);
		int select = 500;
		logger.debug(String.format("Selecting %d oldest photos",select));
		BoundedPriorityQueue<Photo> bpq = new BoundedPriorityQueue<Photo>(select, new PhotoTimeComparator());
		int seen = 0;
		for (Photo photo : this.training) {
			bpq.add(photo);
			seen+=1;
		}
		logger.debug(String.format("Saw %d photos",seen));

		List<Photo> orderedPhotos = bpq.toOrderedList();
		Set<Integer> selectedClusters = new HashSet<Integer>();
		for (Photo photo : orderedPhotos) {
			selectedClusters.add(this.training.getPhotoCluster(photo));
		}
		writeClusterCSV(clusterOut, selectedClusters);
		writePhotoXML(photoOut, selectedClusters);
	}

	private void writePhotoXML(String photoOut, Set<Integer> selectedClusters) {
		try {
			logger.debug("Writing subset XML file");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.newDocument();
			Element photos = doc.createElement("photos");
			for (Integer cluster : selectedClusters) {
				for (Photo p : this.training.get(cluster)) {
					
					photos.appendChild(PhotoUtils.createElement(p,doc));
				}
			}
			doc.appendChild(photos);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(photoOut));
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(source, result);
			
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writeClusterCSV(String clusterOut, Set<Integer> selectedClusters)
			throws FileNotFoundException, IOException {
		logger.debug("Writing subset CSV file");
		CSVPrinter printer = new CSVPrinter(new FileOutputStream(clusterOut, false));
		printer.changeDelimiter('\t');
		printer.writeln(new String[]{"cluster","document_id"});
		for (Integer cluster : selectedClusters) {
			for (Photo p : this.training.get(cluster)) {				
				printer.writeln(new String[]{String.format("%d",cluster),String.format(p.getId())});
			}
		}
		printer.flush();
		printer.close();
	}
	
	public static void main(String[] args) throws IOException {
		InputStream clusterfis = new FileInputStream("/home/ss/Experiments/mediaeval/SED2013/sed2013_dataset_train_gs.csv");
		InputStream photosfis = new FileInputStream("/home/ss/Experiments/mediaeval/SED2013/sed2013_dataset_train.xml");
//		InputStream clusterfis = SED2013SubsetGenerator.class.getResourceAsStream("/flickr.photo.cluster.csv");
//		InputStream photosfis =  SED2013SubsetGenerator.class.getResourceAsStream("/flickr.photo.xml");
		new SED2013SubsetGenerator(clusterfis, photosfis,"/home/ss/Experiments/mediaeval/SED2013/sed2013_partial_dataset_train_gs.csv","/home/ss/Experiments/mediaeval/SED2013/sed2013_partial_dataset_train.xml");
	}
}
