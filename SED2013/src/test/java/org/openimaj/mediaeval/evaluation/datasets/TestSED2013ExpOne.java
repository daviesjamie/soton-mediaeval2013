package org.openimaj.mediaeval.evaluation.datasets;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.openimaj.mediaeval.evaluation.cluster.analyser.MEAnalysis;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne.Training;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestSED2013ExpOne {
	private Training ds;
	private SED2013ExpOne expOne;

	@Before
	public void before() throws IOException{
		expOne = new SED2013ExpOne();
		ds = new Training(
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.cluster.csv"),
			TestSED2013ExpOne.class.getResourceAsStream("/flickr.photo.xml")
		);
	}

	@Test
	public void testEval(){
		MEAnalysis res = expOne.eval(ds);
		System.out.println(res.getSummaryReport());
	}
}
