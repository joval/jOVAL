// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.wmi;

import org.joval.os.windows.wmi.WmiException;

/**
 * An interface to WMI for performing queries.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWmiProvider {
    public static final String CIMv2 = "root\\cimv2";

    /**
     * Connect to the underlying WMI provider.
     */
    public boolean connect();

    /**
     * Release any resources being consumed by the WMI provider.
     */
    public void disconnect();

    /**
     * Execute a WQL query on the given namespace.
     */
    public ISWbemObjectSet execQuery(String ns, String wql) throws WmiException;
}
