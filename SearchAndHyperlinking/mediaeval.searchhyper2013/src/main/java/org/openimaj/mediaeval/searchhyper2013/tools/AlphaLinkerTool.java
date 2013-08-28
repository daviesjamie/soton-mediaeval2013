package org.openimaj.mediaeval.searchhyper2013.tools;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.AnchorList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.linker.AlphaLinker;
import org.openimaj.mediaeval.searchhyper2013.linker.LinkerException;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.xml.sax.SAXException;

public class AlphaLinkerTool {

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, LinkerException {
		AnchorList anchors = AnchorList.readFromFile(new File(args[0]));
		
		LSHDataExplorer lshExplorer = new LSHDataExplorer(new File(args[1]), 3);
		
		AlphaLinker linker = new AlphaLinker("AlphaLinker", new File(args[2]), lshExplorer);
		
		for (Anchor anchor : anchors) {
			System.out.println(anchor + " : ");
			
			ResultList results = linker.link(anchor);
			
			System.out.println(results + "\n----");
		}
	}

}
