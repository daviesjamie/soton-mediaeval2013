package com.github.wcerfgba.adhocstructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * An ad-hoc data structure backed by an Object[][].
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class SemanticTable implements Collection<Object[]> {
	Object[][] table;
	
	int rowCap;
	int colCap;
	
	List<AddHandler<Object[]>> addHandlers;
	List<IdentifyRequestHandler<Object, Integer>> idHandlers;
	
	public SemanticTable(int cols) {
		colCap = cols;
		rowCap = 10;
		
		init();
	}
	
	void init() {		
		table = new Object[rowCap][colCap];
		
		for (int i = 0; i < rowCap; i++) {
			table[i] = new Object[colCap];
		}
		
		addHandlers = new ArrayList<AddHandler<Object[]>>();
		idHandlers = new ArrayList<IdentifyRequestHandler<Object, Integer>>();
	}
	
	public SemanticTable(SemanticTable other) {
		table = Arrays.copyOf(other.table, other.rowCap);
		
		rowCap = other.rowCap;
		colCap = other.colCap;
		
		addHandlers = new ArrayList<AddHandler<Object[]>>(other.addHandlers);
		idHandlers = new ArrayList<IdentifyRequestHandler<Object, Integer>>(other.idHandlers);
	}
	
	@Override
	public boolean add(Object[] arg0) {
		for (AddHandler<Object[]> handler : addHandlers) {
			if (handler.handleAdd(arg0)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Object[]> arg0) {
		boolean changed = false;
		
		for (Object[] arr : arg0) {
			changed |= add(arr);
		}
		
		return changed;
	}

	@Override
	public void clear() {
		table = new Object[10][colCap];
	}

	@Override
	public boolean contains(Object arg0) {
		int size = size();
		
		for (IdentifyRequestHandler<Object, Integer> handler : idHandlers) {
			int pos = handler.handleIdentifyRequest(arg0);
			
			if (-1 < pos && pos < size) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		for (Object obj : arg0) {
			if (!contains(obj)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public boolean isEmpty() {
		return size() > 0;
	}

	@Override
	public Iterator<Object[]> iterator() {
		return Lists.newArrayList(table).iterator();
	}

	@Override
	public boolean remove(Object arg0) {
		int pos = getRowIndex(arg0);
		
		if (-1 < pos && pos < size()) {
			for (int i = 0; i < colCap; i++) {
				table[pos][i] = null;
			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		boolean changed = false;
		
		for (Object obj : arg0) {
			changed |= remove(obj);
		}
		
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		boolean changed = false;
		
		for (Object obj : this) {
			if (!arg0.contains(obj)) {
				remove(obj);
				
				changed = true;
			}
		}
		
		return changed;
	}

	@Override
	public int size() {
		int size = 0;
		
		row:
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < colCap; j++) {
				if (table[i][j] != null) {
					size++;
					
					continue row;
				}
			}
		}
		
		return size;
	}

	@Override
	public Object[] toArray() {
		return table;
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return (T[]) table;
	}

	public boolean addAddHandler(AddHandler<Object[]> addHandler) {
		return addHandlers.add(addHandler);
	}

	public Object[] get(Object obj) {
		int pos = getRowIndex(obj);
		
		if (pos != -1) {
			return table[pos];
		} else {
			return null;
		}
	}

	public void set(Object id, Object[] row) {
		int size = size();
		
		int pos = getRowIndex(id);
		
		if (-1 >= pos || pos >= size) {
			if (size >= rowCap) {
				growRows();
			}
			
			pos = size();
		}
		
		for (int i = 0; i < colCap; i++) {
			if (i < row.length) {
				table[pos][i] = row[i];
			} else {
				table[pos][i] = null;
			}
		}
	}
	
	public int getRowIndex(Object obj) {
		int size = size();
		
		for (IdentifyRequestHandler<Object, Integer> handler : idHandlers) {
			int pos = handler.handleIdentifyRequest(obj);
			
			if (-1 < pos && pos < size) {
				return pos;
			}
		}
		
		return -1;
	}

	public boolean addIdentifyRequestHandler(
						IdentifyRequestHandler<? extends Object, Integer> idHandler) {
		return idHandlers.add((IdentifyRequestHandler<Object, Integer>) idHandler);
	}

	public Object[] getColumn(int col) {
		int size = size();
		
		Object[] column = new Object[size];
		
		for (int i = 0; i < size; i++) {
			column[i] = table[i][col];
		}
		
		return column;
	}
	
	void growRows() {
		table = Arrays.copyOf(table, 2 * rowCap);
		
		for (int i = rowCap; i < table.length; i++) {
			table[i] = new Object[colCap];
		}
		
		rowCap *= 2;
	}

	public void sort(Comparator<Object[]> comparator) {
		Arrays.sort(table, comparator);
	}

	public Object[] getRow(int row) {
		return table[row];
	}
	
	@Override
	public String toString() {
		final int LEFT_PADDING = 1;
		final int RIGHT_PADDING = 1;
		
		int[] colWidths = new int[colCap];
		
		for (int i = 0; i < rowCap; i++) {
			for (int j = 0; j < colCap; j++) {
				int cellWidth = 0;
				
				if (table[i][j] != null) {
					cellWidth = table[i][j].toString().length();
				}
				
				colWidths[j] = Math.max(colWidths[j], cellWidth);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		
		appendRowSeparator(colWidths, sb, LEFT_PADDING, RIGHT_PADDING);
		
		for (int i = 0; i < rowCap; i++) {
			sb.append("|");
			
			for (int j = 0; j < colCap; j++) {
				int cellWidth = 0;
				String cellString = "";
				
				if (table[i][j] != null) {
					cellString = table[i][j].toString();
					cellWidth = cellString.length();
				}
				
				for (int p = 0; p < LEFT_PADDING; p++) {
					sb.append(" ");
				}
				
				sb.append(cellString);
				
				int remaining = colWidths[j] - cellWidth;
				
				for (int p = 0; p < remaining + RIGHT_PADDING; p++) {
					sb.append(" ");
				}
				
				sb.append("|");
			}
			
			sb.append("\n");
			
			appendRowSeparator(colWidths, sb, LEFT_PADDING, RIGHT_PADDING);
		}
		
		return sb.toString();
	}
	
	private void appendRowSeparator(int[] colWidths, StringBuilder sb, int leftPadding, int rightPadding) {
		for (int col = 0; col < colWidths.length; col++) {
			sb.append("+");
			
			for (int i = 0; i < leftPadding + colWidths[col] + rightPadding; i++) {
				sb.append("-");
			}
		}
		
		sb.append("+\n");
	}
}
