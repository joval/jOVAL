// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.util.HashSet;
import java.util.Hashtable;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * Complex type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RecordType implements IType<RecordType> {
    private Hashtable<String, IType<?>> data;

    public RecordType() {
	data = new Hashtable<String, IType<?>>();
    }

    public IType getField(String name) {
	return data.get(name);
    }

    public void addField(String name, IType<?> field) {
	data.put(name, field);
    }

    // Implement ITyped

    public SimpleDatatypeEnumeration getType() {
	return null; // this is a complex type!
    }

    // Implement Comparable

    public int compareTo(RecordType other) {
	HashSet<String> keys = new HashSet<String>();
	for (String key : data.keySet()) {
	    keys.add(key);
	}
	for (String key : other.data.keySet()) {
	    keys.add(key);
	}
	for (String key : keys) {
	    @SuppressWarnings("unchecked")
	    IType<Object> t1 = (IType<Object>)data.get(key);
	    @SuppressWarnings("unchecked")
	    IType<Object> t2 = (IType<Object>)other.data.get(key);
	    if (t1 != null && t2 != null) {
		if (t1.getClass().getName().equals(t2.getClass().getName())) {
		    switch(t1.compareTo(t2)) {
		      case 0:
			break;
		      default:
			return 1;
		    }
		} else {
		    return 1;
		}
	    } else {
		return 1;
	    }
	}
	return 0;
    }
}
