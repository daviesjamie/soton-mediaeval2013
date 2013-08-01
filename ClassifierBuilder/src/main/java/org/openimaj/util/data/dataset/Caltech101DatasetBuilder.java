package org.openimaj.util.data.dataset;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101.Record;
import org.openimaj.util.string.CaseInsensitiveString;

/**
 * Builds a version of Caltech101 with(out) the specified categories.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Caltech101DatasetBuilder implements
		DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> {

	@Override
	/**
	 * @param args  an optional mode specifier (mode=exclude), followed by 
	 * 				the names of categories to include in the dataset
	 */
	public GroupedDataset<String, ListDataset<MBFImage>, MBFImage> build(String[] args, int maxSize) throws BuildException {
		VFSGroupDataset<Record<MBFImage>> ct101;
		
		try {
			 ct101 = Caltech101.getData(ImageUtilities.MBFIMAGE_READER);
		} catch (IOException e) {
			throw new BuildException(e);
		}

		System.out.println("Got Caltech101: " + ct101.numInstances() + " images");
		
		MapBackedDataset<String, ListDataset<MBFImage>, MBFImage> dataset = 
				new MapBackedDataset<String, ListDataset<MBFImage>, MBFImage>();
		
		if (args == null || args.length == 0) {
			// No args, just return the whole thing.
			for (String key : ct101.keySet()) {
				ListDataset<MBFImage> keyDataset = new Caltech101RecordUnwrapperListDataset<MBFImage>(ct101.get(key));
				
				dataset.put(key, keyDataset);
			}
		} else {
			int start = 0;
			boolean exclude = false;

			// Check for a mode specifier at position 0.
			String[] modeSpec = args[0].split("=");
			
			if (modeSpec[0].equals("mode")) {
				start++;
				
				if (modeSpec.length == 2 && modeSpec[1].equals("exclude")) {
					exclude = true;
				}
			}
			
			// Exclude mode: add all categories to the dataset initially, then 
			// remove.
			if (exclude) {
				for (String key : ct101.keySet()) {
					ListDataset<MBFImage> keyDataset = new Caltech101RecordUnwrapperListDataset<MBFImage>(ct101.get(key));
					
					dataset.put(key, keyDataset);
				}
			}
			
			for (int i = start; i < args.length; i++) {
				CaseInsensitiveString key = new CaseInsensitiveString(args[i]);
				
				if (ct101.containsKey(key)) {
					if (!exclude) {
						ListDataset<MBFImage> keyDataset = new Caltech101RecordUnwrapperListDataset<MBFImage>(ct101.get(key));
						
						dataset.put(key.toString(), keyDataset);
					} else {
						dataset.remove(key.toString());
					}
				} else {
					throw new BuildException("Invalid category: "+ args[i]);
				}
			}
		}
		
		System.out.println("Built dataset: " + dataset.numInstances() + " images"); 
		
		return dataset;
	}

	/**
	 * Wraps a ListDataset<Record<IMAGE>> to present it as a ListDataset<IMAGE>.
	 * 
	 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
	 *
	 * @param <IMAGE>
	 */
	private static class Caltech101RecordUnwrapperListDataset<IMAGE extends Image<?, IMAGE>> implements ListDataset<IMAGE> {
		private ListDataset<Record<IMAGE>> ct101;
		
		public Caltech101RecordUnwrapperListDataset(ListDataset<Record<IMAGE>> ct101) {
			this.ct101 = ct101;
		}

		@Override
		public IMAGE getRandomInstance() {
			return ct101.getRandomInstance().getImage();
		}

		@Override
		public int numInstances() {
			return ct101.numInstances();
		}

		@Override
		public Iterator<IMAGE> iterator() {
			return new Iterator<IMAGE>() {
				Iterator<Record<IMAGE>> ct101Iterator = ct101.iterator();

				@Override
				public boolean hasNext() {
					return ct101Iterator.hasNext();
				}

				@Override
				public IMAGE next() {
					return ct101Iterator.next().getImage();
				}

				@Override
				public void remove() {
					ct101Iterator.remove();
				}
				
			};
		}

		@Override
		public boolean add(IMAGE e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(int index, IMAGE element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends IMAGE> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends IMAGE> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object o) {
			Iterator<IMAGE> iter = iterator();
			
			while (iter.hasNext()) {
				if (iter.next().equals(o)) {
					return true;
				}
			}
			
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!contains(o)) {
					return false;
				}
			}
			
			return true;
		}

		@Override
		public IMAGE get(int index) {
			return ct101.get(index).getImage();
		}

		@Override
		public int indexOf(Object o) {
			Iterator<IMAGE> iter = iterator();
			
			for (int i = 0; iter.hasNext(); i++) {
				if (iter.next().equals(o)) {
					return i;
				}
			}
			
			return -1;
		}

		@Override
		public boolean isEmpty() {
			return ct101.isEmpty();
		}

		@Override
		public int lastIndexOf(Object o) {
			Iterator<IMAGE> iter = iterator();
			
			int lastIndex = -1;
			
			for (int i = 0; iter.hasNext(); i++) {
				if (iter.next().equals(o)) {
					lastIndex = i;
				}
			}
			
			return lastIndex;
		}

		@Override
		public ListIterator<IMAGE> listIterator() {
			return new Caltech101RecordUnwrapperListIterator<IMAGE>(ct101.listIterator());
		}

		@Override
		public ListIterator<IMAGE> listIterator(int index) {
			if (index < 0 || index > ct101.size() - 1) {
				throw new IndexOutOfBoundsException();
			}
			
			ListIterator<IMAGE> iter = listIterator();
			
			for (int i = 0; i < index; i++) {
				iter.next();
			}
			
			return iter;
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IMAGE remove(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IMAGE set(int index, IMAGE element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return ct101.size();
		}

		@Override
		public List<IMAGE> subList(int fromIndex, int toIndex) {
			return new Caltech101RecordUnwrapperList<IMAGE>(ct101.subList(fromIndex, toIndex));
		}

		@Override
		public Object[] toArray() {
			return new Caltech101RecordUnwrapperList<IMAGE>(ct101).toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return new Caltech101RecordUnwrapperList<IMAGE>(ct101).toArray(a);
		}

		@Override
		public IMAGE getInstance(int index) {
			return get(index);
		}

		
	}

	/**
	 * Wraps a List<Record<IMAGE>> to present it as a List<IMAGE>.
	 * 
	 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
	 *
	 * @param <IMAGE>
	 */
	private static class Caltech101RecordUnwrapperList<IMAGE extends Image<?, IMAGE>> implements List<IMAGE> {
		private List<Record<IMAGE>> ct101;
		
		public Caltech101RecordUnwrapperList(List<Record<IMAGE>> ct101) {
			this.ct101 = ct101;
		}

		@Override
		public Iterator<IMAGE> iterator() {
			return new Iterator<IMAGE>() {
				Iterator<Record<IMAGE>> ct101Iterator = ct101.iterator();

				@Override
				public boolean hasNext() {
					return ct101Iterator.hasNext();
				}

				@Override
				public IMAGE next() {
					return ct101Iterator.next().getImage();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}

		@Override
		public boolean add(IMAGE e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(int index, IMAGE element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends IMAGE> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends IMAGE> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object o) {
			Iterator<IMAGE> iter = iterator();
			
			while (iter.hasNext()) {
				if (iter.next().equals(o)) {
					return true;
				}
			}
			
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!contains(o)) {
					return false;
				}
			}
			
			return true;
		}

		@Override
		public IMAGE get(int index) {
			return ct101.get(index).getImage();
		}

		@Override
		public int indexOf(Object o) {
			Iterator<IMAGE> iter = iterator();
			
			for (int i = 0; iter.hasNext(); i++) {
				if (iter.next().equals(o)) {
					return i;
				}
			}
			
			return -1;
		}

		@Override
		public boolean isEmpty() {
			return ct101.isEmpty();
		}

		@Override
		public int lastIndexOf(Object o) {
			Iterator<IMAGE> iter = iterator();
			
			int lastIndex = -1;
			
			for (int i = 0; iter.hasNext(); i++) {
				if (iter.next().equals(o)) {
					lastIndex = i;
				}
			}
			
			return lastIndex;
		}

		@Override
		public ListIterator<IMAGE> listIterator() {
			return new Caltech101RecordUnwrapperListIterator<IMAGE>(ct101.listIterator());
		}

		@Override
		public ListIterator<IMAGE> listIterator(int index) {
			if (index < 0 || index > ct101.size() - 1) {
				throw new IndexOutOfBoundsException();
			}
			
			ListIterator<IMAGE> iter = listIterator();
			
			for (int i = 0; i < index; i++) {
				iter.next();
			}
			
			return iter;
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IMAGE remove(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IMAGE set(int index, IMAGE element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return ct101.size();
		}

		@Override
		public List<IMAGE> subList(int fromIndex, int toIndex) {
			return new Caltech101RecordUnwrapperList<IMAGE>(ct101.subList(fromIndex, toIndex));
		}

		@Override
		public Object[] toArray() {
			Object[] array = new Object[size()];
			
			for (int i = 0; i < array.length; i++) {
				array[i] = ct101.get(i).getImage();
			}
			
			return array;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			if (a.length >= size()) {
				for (int i = 0; i < a.length; i++) {
					a[i] = (T) ct101.get(i).getImage();
				}
				
				return a;
			} else {
				return (T[]) toArray();
			}
		}
	}
	
	private static class Caltech101RecordUnwrapperListIterator<IMAGE extends Image<?, IMAGE>> implements ListIterator<IMAGE> {
		ListIterator<Record<IMAGE>> ct101Iterator;
		
		public Caltech101RecordUnwrapperListIterator(ListIterator<Record<IMAGE>> iter) {
			ct101Iterator = iter;
		}

		@Override
		public boolean hasNext() {
			return ct101Iterator.hasNext();
		}

		@Override
		public IMAGE next() {
			return ct101Iterator.next().getImage();
		}

		@Override
		public void remove() {
			ct101Iterator.remove();
		}

		@Override
		public void add(IMAGE e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasPrevious() {
			return ct101Iterator.hasPrevious();
		}

		@Override
		public int nextIndex() {
			return ct101Iterator.nextIndex();
		}

		@Override
		public IMAGE previous() {
			return ct101Iterator.previous().getImage();
		}

		@Override
		public int previousIndex() {
			return ct101Iterator.previousIndex();
		}

		@Override
		public void set(IMAGE e) {
			throw new UnsupportedOperationException();
		}
		
	};
}
