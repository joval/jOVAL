// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.wmi.query;

import org.jinterop.dcom.common.JIException;

import com.h9labs.jwbem.SWbemObject;

import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemObject.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SimpleSWbemObject implements ISWbemObject {
    private SWbemObject object;

    SimpleSWbemObject(SWbemObject object) {
	this.object = object;
    }

    // Implement ISWbemObject

    public ISWbemPropertySet getProperties() throws WmiException {
	try {
	    return new SimpleSWbemPropertySet(object.getProperties());
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
}
