// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.wmi;

import java.util.Iterator;
import java.util.Vector;

import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;

import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemObjectSet.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SWbemObjectSet implements ISWbemObjectSet {
    Dispatch dispatch;
    Vector<ISWbemObject> objects;

    public SWbemObjectSet(Dispatch dispatch) {
	this.dispatch = dispatch;
	EnumVariant enumVariant = new EnumVariant(dispatch);
	objects = new Vector<ISWbemObject>();
	while(enumVariant.hasMoreElements()) {
	    objects.add(new SWbemObject(enumVariant.nextElement().toDispatch()));
	}
    }

    // Implement ISWbemObjectSet

    public Iterator<ISWbemObject> iterator() {
	return objects.iterator();
    }

    public int getSize() {
	return objects.size();
    }

    public ISWbemObject getItem(String itemName) throws WmiException {
	throw new WmiException("Not implemented");
    }
}
