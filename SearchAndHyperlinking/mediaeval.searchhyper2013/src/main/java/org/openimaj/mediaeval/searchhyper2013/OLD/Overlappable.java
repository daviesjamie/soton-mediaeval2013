package org.openimaj.mediaeval.searchhyper2013.OLD;

import java.util.Collection;

/**
 * Some objects may 'overlap' each other, for example search results in a video
 * file.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <T> Type by which this object is overlappable.
 */
public interface Overlappable<OUTER extends Collection<? extends Comparable<INNER>>, INNER> {
	
	/**
	 * Test if an object overlaps the interface implementor.
	 * 
	 * @param obj The object that may overlap this object.
	 * @return True if the object overlaps, false otherwise.
	 */
	public boolean overlappedBy(OUTER obj);

	/**
	 * Resolve an overlap with an overlapping object by splitting up the 
	 * overlapped object and returning a collection of the new objects. Thus
	 * 
	 *   cd  ghi  l      Overlapping object
	 * abc efg ijk       Overlapped object
	 * 
	 * will become
	 * 
	 * [ ab, cd, ef, ghi, jk, l ].
	 * 
	 * @param obj Overlapping object to split around.
	 * @return A collection of objects that no longer overlap.
	 */
	public Collection<OUTER> splitOverlap(OUTER obj);

	/**
	 * Clips a collection around this overlap, so that all elements in the 
	 * returned collection fall within the bounds of this object.
	 *  
	 * @param collection The collection to clip.
	 * @return A clipped collection.
	 */
	public Collection<OUTER> clipCollection(Collection<OUTER> collection);
}
