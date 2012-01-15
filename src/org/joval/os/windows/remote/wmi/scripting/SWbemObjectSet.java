// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi.scripting;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.jinterop.dcom.common.JIException;

import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemObjectSet.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SWbemObjectSet implements ISWbemObjectSet {
    com.h9labs.jwbem.SWbemObjectSet<com.h9labs.jwbem.SWbemObject> objectSet;

    public SWbemObjectSet(com.h9labs.jwbem.SWbemObjectSet<com.h9labs.jwbem.SWbemObject> objectSet) {
	this.objectSet = objectSet;
    }

    // Implement ISWbemObjectSet

    public Iterator<ISWbemObject> iterator() {
	return new SWbemObjectIterator(objectSet.iterator());
    }

    public int getSize() {
	return objectSet.getSize();
    }

    public ISWbemObject getItem(String itemName) throws WmiException {
	try {
	    return new SWbemObject(objectSet.getItem(itemName));
	} catch (Exception e) {
	    throw new WmiException(e);
	}
    }

    class SWbemObjectIterator implements Iterator<ISWbemObject> {
	Iterator<com.h9labs.jwbem.SWbemObject> iter;

	SWbemObjectIterator(Iterator<com.h9labs.jwbem.SWbemObject> iter) {
	    this.iter = iter;
	}

	public boolean hasNext() {
	    if (iter == null) {
		return false;
	    } else {
		return iter.hasNext();
	    }
	}

	public ISWbemObject next() throws NoSuchElementException {
	    return new SWbemObject(iter.next());
	}

	public void remove() throws UnsupportedOperationException {
	    throw new UnsupportedOperationException("remove");
	}
    }

}
