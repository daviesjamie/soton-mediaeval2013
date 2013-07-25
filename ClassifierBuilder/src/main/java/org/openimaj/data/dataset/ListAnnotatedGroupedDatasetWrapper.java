package org.openimaj.data.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.ml.annotation.Annotated;
import org.openimaj.ml.annotation.AnnotatedObject;

import com.google.common.collect.Iterators;

/**
 * Wraps a GroupedDataset and presents it as a List<Annotated>. This class is 
 * immutable.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <OBJECT>
 * @param <ANNOTATION>
 */
public class ListAnnotatedGroupedDatasetWrapper<OBJECT, ANNOTATION>
							implements List<Annotated<OBJECT, ANNOTATION>> {
	
	GroupedDataset<ANNOTATION, ? extends Dataset<OBJECT>, OBJECT> dataset;
	
	public ListAnnotatedGroupedDatasetWrapper(GroupedDataset<ANNOTATION, ? extends Dataset<OBJECT>, OBJECT> dataset) {
		this.dataset = dataset;
	}

	@Override
	public boolean add(Annotated<OBJECT, ANNOTATION> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int arg0, Annotated<OBJECT, ANNOTATION> arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(
			Collection<? extends Annotated<OBJECT, ANNOTATION>> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int arg0,
			Collection<? extends Annotated<OBJECT, ANNOTATION>> arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object arg0) {
		if (!(arg0 instanceof Annotated)) return false;
		
		Annotated<OBJECT, ANNOTATION> annotated = (Annotated<OBJECT, ANNOTATION>) arg0;
		
		for (ANNOTATION annotation : annotated.getAnnotations()) {
			if (dataset.containsKey(annotation)) {
				for (OBJECT object : dataset.get(annotation)) {
					if (object.equals(annotated.getObject())) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		for (Object o : arg0) {
			if (!contains(o)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Annotated<OBJECT, ANNOTATION> get(int arg0) {
		if (arg0 < 0 || arg0 > size() - 1) {
			throw new IndexOutOfBoundsException();
		}
		
		// Determine collection item is in.
		ANNOTATION key = null;
		
		Iterator<ANNOTATION> keyIter = dataset.keySet().iterator();
		int startNext = 0;
		int startCur = startNext;
		
		while (startNext <= arg0 && keyIter.hasNext()) {
			key = keyIter.next();
			startCur = startNext;
			startNext += dataset.get(key).numInstances();
		}
		
		int indexWithinGroup = arg0 - startCur;
		
		// If it's a ListDataset with our GroupedDataset, get it directly.
		if (dataset.get(key) instanceof ListDataset) {
			return new AnnotatedObject<OBJECT, ANNOTATION>(((ListDataset<OBJECT>) dataset.get(key)).get(indexWithinGroup), key);
		}
		
		Iterator<OBJECT> iter = dataset.get(key).iterator();
		for (int i = 0; i < indexWithinGroup; i++) {
			iter.next();
		}
		
		return new AnnotatedObject<OBJECT, ANNOTATION>(iter.next(), key);
		
		
	}

	@Override
	public int indexOf(Object arg0) {
		if (!(arg0 instanceof Annotated)) return -1;
		
		Annotated<OBJECT, ANNOTATION> annotated = (Annotated<OBJECT, ANNOTATION>) arg0;
		
		if (annotated.getAnnotations().isEmpty()) return -1;
		
		ANNOTATION annotation = annotated.getAnnotations().iterator().next();
		
		int index = 0;
		
		for (ANNOTATION key : dataset.keySet()) {
			if (!(key.equals(annotation))) {
				index += dataset.get(key).numInstances();
			} else {
				break;
			}
		}
		
		for (OBJECT obj : dataset.get(annotation)) {
			if (!(obj.equals(annotated.getObject()))) {
				index++;
			} else {
				break;
			}
		}
		
		return index;
	}

	@Override
	public boolean isEmpty() {
		return dataset.isEmpty();
	}

	@Override
	public Iterator<Annotated<OBJECT, ANNOTATION>> iterator() {
		class AnnotatedIterator implements Iterator<Annotated<OBJECT, ANNOTATION>> {
			private Iterator<OBJECT> iter;
			private ANNOTATION annotation;
			
			public AnnotatedIterator(Iterator<OBJECT> iter, ANNOTATION annotation) {
				this.iter = iter;
				this.annotation = annotation;
			}
			
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Annotated<OBJECT, ANNOTATION> next() {
				return new AnnotatedObject<OBJECT, ANNOTATION>(iter.next(), annotation);
			}

			@Override
			public void remove() {
				iter.remove();
			}
			
		}
		
		Iterator<Annotated<OBJECT, ANNOTATION>> iter = Iterators.emptyIterator();
		
		for (ANNOTATION annotation : dataset.keySet()) {
			iter = Iterators.concat(iter, new AnnotatedIterator(dataset.get(annotation).iterator(), annotation));
		}
		
		return iter;
	}

	@Override
	public int lastIndexOf(Object arg0) {
		if (!(arg0 instanceof Annotated)) {
			return -1;
		}
		
		Iterator<Annotated<OBJECT, ANNOTATION>> iter = iterator();
		
		int lastIndex = 0;
		
		for (int i = 0; iter.hasNext(); i++) {
			if (iter.next().equals(arg0)) {
				lastIndex = i;
			}
		}
		
		return lastIndex;
	}

	@Override
	public ListIterator<Annotated<OBJECT, ANNOTATION>> listIterator() {
		return new ListIterator<Annotated<OBJECT, ANNOTATION>>() {
			List<Annotated<OBJECT, ANNOTATION>> cache = new ArrayList<Annotated<OBJECT, ANNOTATION>>();
			
			Iterator<Annotated<OBJECT, ANNOTATION>> iter = iterator();
			ListIterator<Annotated<OBJECT, ANNOTATION>> cacheIter = cache.listIterator();
			
			@Override
			public void add(Annotated<OBJECT, ANNOTATION> e) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean hasNext() {
				return cacheIter.hasNext() || iter.hasNext();
			}
			@Override
			public boolean hasPrevious() {
				return cacheIter.hasPrevious();
			}
			@Override
			public Annotated<OBJECT, ANNOTATION> next() {
				if (!cacheIter.hasNext()) {
					cacheIter.add(iter.next());
				}
				
				return cacheIter.next();
			}
			@Override
			public int nextIndex() {
				return cacheIter.nextIndex();
			}
			@Override
			public Annotated<OBJECT, ANNOTATION> previous() {
				return cacheIter.previous();
			}
			@Override
			public int previousIndex() {
				return cacheIter.previousIndex();
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void set(Annotated<OBJECT, ANNOTATION> e) {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public ListIterator<Annotated<OBJECT, ANNOTATION>> listIterator(int arg0) {
		if (arg0 < 0 || arg0 > size()) {
			throw new IndexOutOfBoundsException();
		}
		
		ListIterator<Annotated<OBJECT, ANNOTATION>> iter = listIterator();
		
		for (int i = 0; i < arg0; i++) {
			iter.next();
		}
		
		return iter;
	}

	@Override
	public boolean remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Annotated<OBJECT, ANNOTATION> remove(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Annotated<OBJECT, ANNOTATION> set(int arg0,
			Annotated<OBJECT, ANNOTATION> arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		int size = 0;
		
		for (ANNOTATION key : dataset.keySet()) {
			size += dataset.get(key).numInstances();
		}
		
		return size;
	}

	@Override
	public List<Annotated<OBJECT, ANNOTATION>> subList(int arg0, int arg1) {
		ListIterator<Annotated<OBJECT, ANNOTATION>> iter = listIterator(arg0);
		
		List<Annotated<OBJECT, ANNOTATION>> list = new ArrayList<Annotated<OBJECT, ANNOTATION>>(arg1 - arg0);
		
		for (int i = arg0; i < arg1; i++) {
			list.add(iter.next());
		}
		
		return list;
	}

	@Override
	public Object[] toArray() {
		return subList(0, size()).toArray();
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return subList(0, size()).toArray(arg0);
	}

}
