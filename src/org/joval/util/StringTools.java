// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An embarassingly stupid class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StringTools {
    /**
     * Sort the array from A->Z (ascending ordering).
     */
    public static final String[] sort(String[] array) {
	return sort(array, true);
    }

    /**
     * Arrays can be sorted ascending or descending.
     *
     * @param asc true for ascending (A->Z), false for descending (Z->A).
     */
    public static final String[] sort(String[] array, boolean asc) {
	Arrays.sort(array, new StringComparator(asc));
	return array;
    }

    /**
     * A StringTokenizer operates on single-character tokens.  This acts on a delimiter that is a multi-character
     * String.
     */
    public static Iterator <String>tokenize(String target, String delimiter) {
	return new StringTokenIterator(target, delimiter);
    }

    // Private

    static final class StringComparator implements Comparator<String> {
	boolean ascending = true;

	StringComparator (boolean asc) {
	    this.ascending = asc;
	}

	public int compare(String s1, String s2) {
	    if (ascending) {
		return s1.compareTo(s2);
	    } else {
		return s2.compareTo(s1);
	    }
	}

	public boolean equals(Object obj) {
	    if (obj instanceof StringComparator) {
		return ascending == ((StringComparator)obj).ascending;
	    }
	    return false;
	}
    }

    static final class StringTokenIterator implements Iterator <String> {
	private String target, delimiter, next;
	int pointer;

	StringTokenIterator(String target, String delimiter) {
	    //
	    // Trim tokens from the beginning and end.
	    //
	    int len = delimiter.length();
	    if (target.startsWith(delimiter)) {
		target = target.substring(len);
	    }
	    if (target.endsWith(delimiter)) {
		target = target.substring(0, target.length() - len);
	    }

	    this.target = target;
	    this.delimiter = delimiter;
	    pointer = 0;
	}

	public boolean hasNext() {
	    if (next == null) {
		try {
		    next = next();
		} catch (NoSuchElementException e) {
		    return false;
		}
	    }
	    return true;
	}

	public String next() throws NoSuchElementException {
	    if (next != null) {
		String tmp = next;
		next = null;
		return tmp;
	    }
	    int i = target.indexOf(delimiter, pointer);
	    if (i > 0) {
		String tmp = target.substring(pointer, i);
		pointer = (i + delimiter.length());
		return tmp;
	    } else if (pointer < target.length()) {
		String tmp = target.substring(pointer);
		pointer = target.length();
		return tmp;
	    }
	    throw new NoSuchElementException("No tokens after " + pointer);
	}

	public void remove() {
	    throw new UnsupportedOperationException("Remove not supported");
	}
    }
}
