package org.openimaj.mediaeval.evaluation.cluster;

import java.util.Comparator;
import java.util.Map.Entry;

import org.openimaj.util.pair.IntDoublePair;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class MostStableComparator implements Comparator<Entry<Integer, IntDoublePair>> {

	@Override
	public int compare(Entry<Integer, IntDoublePair> o1,Entry<Integer, IntDoublePair> o2) {
		double stab1 = o1.getValue().second;
		double stab2 = o2.getValue().second;
		double stab1ind = o1.getValue().first;
		double stab2ind = o2.getValue().first;
		if(stab1ind == -1)
			return 1;
		if(stab2ind == -1)
			return -1;
		return -Double.compare(stab1, stab2);
	}

}
