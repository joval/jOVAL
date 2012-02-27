// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import oval.schemas.systemcharacteristics.core.ItemType;

/**
 * A Set of ItemType objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ItemSet <T extends ItemType> {
    private Hashtable<BigInteger, T> table;

    /**
     * Construct an empty set.
     */
    public ItemSet() {
	table = new Hashtable<BigInteger, T>();
    }

    public ItemSet(Collection<T> items) {
	this();
	for (T item : items) {
	    table.put(item.getId(), item);
	}
    }

    public String toString() {
	StringBuffer sb = new StringBuffer("Set [ ");
	List<T> items = toList();
	int len = items.size();
	for (int i=0; i < len; i++) {
	    T item = items.get(i);
	    if (i > 0) {
		sb.append(", ");
	    }
	    sb.append(item.getId());
	}
	sb.append(" ]");
	return sb.toString();
    }

    public ItemSet<T> union(ItemSet<? extends T> other) {
	Hashtable<BigInteger, T> temp = new Hashtable<BigInteger, T>();
	for (T item : table.values()) {
	    temp.put(item.getId(), item);
	}
	for (T item : other.table.values()) {
	    temp.put(item.getId(), item);
	}
	return new ItemSet<T>(temp);
    }

    public ItemSet<T> intersection(ItemSet<? extends T> other) {
	Hashtable<BigInteger, T> temp = new Hashtable<BigInteger, T>();
	for (T item : table.values()) {
	    if (other.table.containsKey(item.getId())) {
		temp.put(item.getId(), item);
	    }
	}
	return new ItemSet<T>(temp);
    }

    /**
     * A.complement(B) is the set of everything in A that is not in B.
     */
    public ItemSet<T> complement(ItemSet<? extends T> other) {
	Hashtable<BigInteger, T> temp = new Hashtable<BigInteger, T>();
	for (T item : table.values()) {
	    if (!other.table.containsKey(item.getId())) {
		temp.put(item.getId(), item);
	    }
	}
	return new ItemSet<T>(temp);
    }

    public List<T> toList() {
	Vector<T> v = new Vector<T>();
	v.addAll(table.values());
	return v;
    }

    // Private

    private ItemSet(Hashtable<BigInteger, T> table) {
	this.table = table;
    }
}
