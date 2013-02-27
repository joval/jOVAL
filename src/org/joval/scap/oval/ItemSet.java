// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Adler32;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import scap.oval.systemcharacteristics.core.ItemType;

import org.joval.xml.SchemaRegistry;

/**
 * A Set of ItemType objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ItemSet<T extends ItemType> implements Iterable<T> {
    private Map<String, Collection<T>> table;
    private Marshaller marshaller;

    /**
     * Construct an empty set.
     */
    public ItemSet() {
	table = new HashMap<String, Collection<T>>();
    }

    /**
     * Construct a set containing the specified items.  If the items have IDs, they will form the basis for identifying
     * the items for purposes of item comparisons in set operations.  If the IDs are not set, then comparisons will be
     * performed based upon the /contents/ of the items themselves.
     */
    public ItemSet(Collection<T> items) {
	this();
	addItems(items);
    }

    public String toString() {
	StringBuffer sb = new StringBuffer("Set [");
	for (Map.Entry<String, Collection<T>> entry : table.entrySet()) {
	    if (sb.length() > 5) {
		sb.append(", ");
	    }
	    sb.append(entry.getKey()).append(":").append(Integer.toString(entry.getValue().size()));
	}
	return sb.append("]").toString();
    }

    public ItemSet<T> union(ItemSet<T> other) {
	Map<String, Collection<T>> temp = new HashMap<String, Collection<T>>();
	for (String id : table.keySet()) {
	    temp.put(id, table.get(id));
	}
	Collection<T> collisions = new ArrayList<T>();
	for (String id : other.table.keySet()) {
	    if (temp.containsKey(id)) {
		collisions.addAll(other.table.get(id));
	    } else {
		temp.put(id, other.table.get(id));
	    }
	}
	return new ItemSet<T>(temp).addItems(collisions);
    }

    public ItemSet<T> intersection(ItemSet<T> other) {
	Map<String, Collection<T>> temp = new HashMap<String, Collection<T>>();
	for (Map.Entry<String, Collection<T>> entry : table.entrySet()) {
	    if (other.table.containsKey(entry.getKey())) {
		for (T item : entry.getValue()) {
		    byte[] itemData = toBytes(item);
		    for (T otherItem : other.table.get(entry.getKey())) {
			if (equal(itemData, toBytes(otherItem))) {
			    if (!temp.containsKey(entry.getKey())) {
				temp.put(entry.getKey(), new ArrayList<T>());
			    }
			    temp.get(entry.getKey()).add(item);
			    break;
			}
		    }
		}
	    }
	}
	return new ItemSet<T>(temp);
    }

    /**
     * A.complement(B) is the set of everything in A that is not in B.
     */
    public ItemSet<T> complement(ItemSet<T> other) {
	Map<String, Collection<T>> temp = new HashMap<String, Collection<T>>();
	for (Map.Entry<String, Collection<T>> entry : table.entrySet()) {
	    if (other.table.containsKey(entry.getKey())) {
		for (T item : entry.getValue()) {
		    byte[] itemData = toBytes(item);
		    boolean match = false;
		    for (T otherItem : other.table.get(entry.getKey())) {
			if (equal(itemData, toBytes(otherItem))) {
			    match = true;
			    break;
			}
		    }
		    if (!match) {
			if (!temp.containsKey(entry.getKey())) {
			    temp.put(entry.getKey(), new ArrayList<T>());
			}
			temp.get(entry.getKey()).add(item);
		    }
		}
	    } else {
		temp.put(entry.getKey(), entry.getValue());
	    }
	}
	return new ItemSet<T>(temp);
    }

    /**
     * Convert the ItemSet to a List.
     */
    public List<T> toList() {
	List<T> list = new ArrayList<T>();
	for (Collection<T> items : table.values()) {
	    list.addAll(items);
	}
	return list;
    }

    /**
     * Return the number if items in the set.
     */
    public int size() {
	int size = 0;
	for (Collection<T> coll : table.values()) {
	    size += coll.size();
	}
	return size;
    }

    // Implement Iterable

    public Iterator<T> iterator() {
	return toList().iterator();
    }

    // Private

    private ItemSet(Map<String, Collection<T>> table) {
	this.table = table;
    }

    private static final QName QNAME = new QName("SetItem");

    private Marshaller getMarshaller() {
	if (marshaller == null) {
	    try {
		marshaller = SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.getJAXBContext().createMarshaller();
	    } catch (JAXBException e) {
		throw new RuntimeException(e);
	    }
	}
	return marshaller;
    }

    /**
     * For items with no ID - converts to bytes.
     */
    private byte[] toBytes(ItemType item) {
	try {
	    @SuppressWarnings("unchecked")
	    JAXBElement<ItemType> elt = new JAXBElement(QNAME, item.getClass(), item);
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    getMarshaller().marshal(elt, out);
	    return out.toByteArray();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private boolean equal(byte[] b1, byte[] b2) {
	if (b1.length == b2.length) {
	    for (int i=0; i < b1.length; i++) {
		if (b1[i] != b2[i]) {
		    return false;
		}
	    }
	    return true;
	} else {
	    return false;
	}
    }

    private ItemSet<T> addItems(Collection<T> items) {
	for (T item : items) {
	    if (item.isSetId()) {
		table.put(item.getId().toString(), Arrays.asList(item));
	    } else {
		byte[] data = toBytes(item);
		Adler32 adler = new Adler32();
		adler.update(data);
		String cs = Long.toString(adler.getValue());
		if (table.containsKey(cs)) {
		    //
		    // If another item with the same Adler32 checksum has been stored previously, that doesn't mean
		    // it contains the same data.  So, we compare it to all the previously-stored items with the
		    // same checksum.
		    //
		    boolean match = false;
		    for (ItemType candidate : table.get(cs)) {
			if (equal(data, toBytes(candidate))) {
			    match = true;
			    break;
			}
		    }
		    if (!match) {
			//
			// The new item is unique, so add it.
			//
			table.get(cs).add(item);
		    }
		} else {
		    Collection<T> collection = new ArrayList<T>();
		    collection.add(item);
		    table.put(cs, collection);
		}
	    }
	}
	return this;
    }
}
