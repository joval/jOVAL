// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi.scripting;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

import com.h9labs.jwbem.SWbemServices;

import org.joval.intf.windows.wmi.ISWbemEventSource;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.os.windows.wmi.WmiException;

/**
 * An ISWbemEventSource is a source of ISWbemObjects.
 * 
 * @see "http://msdn.microsoft.com/en-us/library/aa393741(VS.85).aspx"
 */
public class SWbemEventSource implements ISWbemEventSource {
    private IJIDispatch sourceDispatch;
    private SWbemServices services;

    public SWbemEventSource(IJIDispatch dispatch, SWbemServices services) {
	sourceDispatch = dispatch;
	this.services = services;
    }

    // Implement ISWbemEventSource

    public ISWbemObject nextEvent() throws WmiException {
	Object[] inParams = new Object[] {new Integer(3600000)}; // 1 hr
	try {
	    JIVariant[] results = sourceDispatch.callMethodA("NextEvent", inParams);
	    IJIComObject co = results[0].getObjectAsComObject();
	    if (co == null) {
		throw new WmiException("Timeout?");
	    }
	    IJIDispatch dispatch = (IJIDispatch)JIObjectFactory.narrowObject(co);
	    return new SWbemObject(new com.h9labs.jwbem.SWbemObject(dispatch, services));
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
}
