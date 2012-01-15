// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.wmi.scripting;

import java.util.Iterator;
import java.util.Vector;

import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;

import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemPropertySet.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SWbemPropertySet implements ISWbemPropertySet {
    private Dispatch dispatch;
    private Vector<ISWbemProperty> properties;

    SWbemPropertySet(Dispatch dispatch) {
	this.dispatch = dispatch;
	EnumVariant enumVariant = new EnumVariant(dispatch);
	properties = new Vector<ISWbemProperty>();
	while(enumVariant.hasMoreElements()) {
	    properties.add(new SWbemProperty(enumVariant.nextElement().toDispatch()));
	}
    }

    // Implement ISWbemProperties

    public Iterator<ISWbemProperty> iterator() {
	return properties.iterator();
    }

    public int getSize() {
	return properties.size();
    }

    public ISWbemProperty getItem(String itemName) throws WmiException {
	Iterator<ISWbemProperty> iter = iterator();
	while(iter.hasNext()) {
	    ISWbemProperty prop = iter.next();
	    if (itemName.equals(prop.getName())) {
		return prop;
	    }
	}
	return null;
    }
}
