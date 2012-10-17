// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import oval.schemas.systemcharacteristics.core.ItemType;

import org.joval.xml.SchemaRegistry;

/**
 * A Set of ItemType objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ItemSet<T extends ItemType> implements Iterable<T> {
    private Hashtable<String, T> table;
    private Marshaller marshaller;

    /**
     * Construct an empty set.
     */
    public ItemSet() {
	table = new Hashtable<String, T>();
	String packages = SchemaRegistry.lookup(SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS);
	try {
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    marshaller = ctx.createMarshaller();
	} catch (JAXBException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Construct a set containing the specified items.  If the items have IDs, they will form the basis for identifying
     * the items for purposes of item comparisons in set operations.  If the IDs are not set, then comparisons will be
     * performed based upon the /contents/ of the items themselves.
     */
    public ItemSet(Collection<T> items) {
	this();
	for (T item : items) {
	    if (item.isSetId()) {
		table.put(item.getId().toString(), item);
	    } else {
		table.put(getChecksum(item), item);
	    }
	}
    }

    public String toString() {
	StringBuffer sb = new StringBuffer("Set [");
	for (String id : table.keySet()) {
	    if (sb.length() > 5) {
		sb.append(", ");
	    }
	    sb.append(id);
	}
	return sb.append("]").toString();
    }

    public ItemSet<T> union(ItemSet<? extends T> other) {
	Hashtable<String, T> temp = new Hashtable<String, T>();
	for (String id : table.keySet()) {
	    temp.put(id, table.get(id));
	}
	for (String id : other.table.keySet()) {
	    temp.put(id, other.table.get(id));
	}
	return new ItemSet<T>(temp);
    }

    public ItemSet<T> intersection(ItemSet<? extends T> other) {
	Hashtable<String, T> temp = new Hashtable<String, T>();
	for (String id : table.keySet()) {
	    if (other.table.containsKey(id)) {
		temp.put(id, table.get(id));
	    }
	}
	return new ItemSet<T>(temp);
    }

    /**
     * A.complement(B) is the set of everything in A that is not in B.
     */
    public ItemSet<T> complement(ItemSet<? extends T> other) {
	Hashtable<String, T> temp = new Hashtable<String, T>();
	for (String id : table.keySet()) {
	    if (!other.table.containsKey(id)) {
		temp.put(id, table.get(id));
	    }
	}
	return new ItemSet<T>(temp);
    }

    /**
     * Convert the HashSet to a List.
     */
    public List<T> toList() {
	Vector<T> v = new Vector<T>();
	v.addAll(table.values());
	return v;
    }

    // Implement Iterable

    public Iterator<T> iterator() {
	return table.values().iterator();
    }

    // Private

    private ItemSet(Hashtable<String, T> table) {
	this.table = table;
    }

    private static final QName QNAME = new QName("SetItem");

    /**
     * Generate a unique identifier based on the contents of the item.
     */
    private String getChecksum(ItemType item) {
	try {
	    @SuppressWarnings("unchecked")
	    JAXBElement<ItemType> elt = new JAXBElement(QNAME, item.getClass(), item);

	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    marshaller.marshal(elt, out);
	    byte[] buff = out.toByteArray();
	    MessageDigest digest = MessageDigest.getInstance("MD5");
	    digest.update(buff, 0, buff.length);
	    byte[] cs = digest.digest();
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < cs.length; i++) {
		sb.append(Integer.toHexString(0xFF & cs[i]));
	    }
	    return sb.toString();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }
}
