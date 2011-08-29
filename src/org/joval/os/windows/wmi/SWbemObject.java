// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.wmi;

import com.jacob.com.Dispatch;

import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemObject.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SWbemObject implements ISWbemObject {
    private Dispatch dispatch;

    SWbemObject(Dispatch dispatch) {
	this.dispatch = dispatch;
    }

    // Implement ISWbemObject

    public ISWbemPropertySet getProperties() throws WmiException {
	return new SWbemPropertySet(Dispatch.call(dispatch, "Properties_").toDispatch());
    }
}
