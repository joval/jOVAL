// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;

import java.util.HashSet;
import java.util.Hashtable;

import org.joval.intf.oval.IType;

/**
 * Complex type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RecordType extends AbstractType {
    private Hashtable<String, IType> data;

    public RecordType(EntityItemRecordType record) throws IllegalArgumentException {
	this();
	for (EntityItemFieldType field : record.getField()) {
	    data.put(field.getName(), TypeFactory.createType(field));
	}
    }

    public RecordType() {
	data = new Hashtable<String, IType>();
    }

    public IType getField(String name) {
	return data.get(name);
    }

    public void addField(String name, IType field) {
	data.put(name, field);
    }

    // Implement ITyped

    public Type getType() {
	return Type.RECORD;
    }

    public String getString() throws UnsupportedOperationException {
	throw new UnsupportedOperationException("getString()");
    }

    // Implement Comparable

    public int compareTo(IType t) {
	RecordType other = null;
	if (t instanceof RecordType) {
	    other = (RecordType)t;
	} else {
	    throw new IllegalArgumentException(t.getClass().getName());
	}
	HashSet<String> keys = new HashSet<String>();
	for (String key : data.keySet()) {
	    keys.add(key);
	}
	for (String key : other.data.keySet()) {
	    keys.add(key);
	}
	for (String key : keys) {
	    IType t1 = data.get(key);
	    IType t2 = other.data.get(key);
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
