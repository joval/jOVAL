// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi.query;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jinterop.dcom.common.JIException;

import com.h9labs.jwbem.SWbemProperty;
import com.h9labs.jwbem.SWbemPropertySet;

import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemPropertySet.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SimpleSWbemPropertySet implements ISWbemPropertySet {
    private SWbemPropertySet propertySet;

    SimpleSWbemPropertySet(SWbemPropertySet propertySet) {
	this.propertySet = propertySet;
    }

    // Implement ISWbemProperties

    public Iterator<ISWbemProperty> iterator() {
	return new SWbemPropertyIterator(propertySet.iterator());
    }

    public int getSize() {
	return propertySet.getSize();
    }

    public ISWbemProperty getItem(String itemName) throws WmiException {
	try {
	    return new SimpleSWbemProperty(propertySet.getItem(itemName));
	} catch (Exception e) {
	    throw new WmiException(e);
	}
    }

    class SWbemPropertyIterator implements Iterator<ISWbemProperty> {
	Iterator<SWbemProperty> iter;

	SWbemPropertyIterator(Iterator<SWbemProperty> iter) {
	    this.iter = iter;
	}

	public boolean hasNext() {
	    return iter.hasNext();
	}

	public ISWbemProperty next() throws NoSuchElementException {
	    return new SimpleSWbemProperty(iter.next());
	}

	public void remove() throws UnsupportedOperationException {
	    throw new UnsupportedOperationException("remove");
	}
    }

}
