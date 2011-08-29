// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.wmi;

import org.joval.os.windows.wmi.WmiException;

/**
 * An ISWbemObject is a collection of ISWbemProperties.
 * 
 * @see "http://msdn.microsoft.com/en-us/library/aa393741(VS.85).aspx"
 */
public interface ISWbemObject {
    /**
     * Get the object's property set.
     */
    public ISWbemPropertySet getProperties() throws WmiException;
}
