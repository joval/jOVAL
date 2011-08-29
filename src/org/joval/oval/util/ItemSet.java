// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.util;

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
public class ItemSet {
    private Hashtable<BigInteger, ItemType> ht;

    /**
     * Construct an empty set.
     */
    public ItemSet() {
	ht = new Hashtable<BigInteger, ItemType>();
    }

    public ItemSet(Collection<ItemType> items) {
	this();
	for (ItemType item : items) {
	    ht.put(item.getId(), item);
	}
    }

    public String toString() {
	StringBuffer sb = new StringBuffer("Set [ ");
	List<ItemType> items = toList();
	int len = items.size();
	for (int i=0; i < len; i++) {
	    ItemType item = items.get(i);
	    if (i > 0) {
		sb.append(", ");
	    }
	    sb.append(item.getId());
	}
	sb.append(" ]");
	return sb.toString();
    }

    public ItemSet union(ItemSet other) {
	Hashtable<BigInteger, ItemType> temp = new Hashtable<BigInteger, ItemType>();
	for (ItemType item : ht.values()) {
	    temp.put(item.getId(), item);
	}
	for (ItemType item : other.ht.values()) {
	    temp.put(item.getId(), item);
	}
	return new ItemSet(temp);
    }

    public ItemSet intersection(ItemSet other) {
	Hashtable<BigInteger, ItemType> temp = new Hashtable<BigInteger, ItemType>();
	for (ItemType item : ht.values()) {
	    if (other.ht.containsKey(item.getId())) {
		temp.put(item.getId(), item);
	    }
	}
	return new ItemSet(temp);
    }

    /**
     * A.complement(B) is the set of everything in A that is not in B.
     */
    public ItemSet complement(ItemSet other) {
	Hashtable<BigInteger, ItemType> temp = new Hashtable<BigInteger, ItemType>();
	for (ItemType item : toList()) {
	    if (!other.ht.containsKey(item.getId())) {
		temp.put(item.getId(), item);
	    }
	}
	return new ItemSet(temp);
    }

    public List<ItemType> toList() {
	Vector<ItemType> v = new Vector<ItemType>();
	v.addAll(ht.values());
	return v;
    }

    // Private

    private ItemSet(Hashtable<BigInteger, ItemType> ht) {
	this.ht = ht;
    }
}
