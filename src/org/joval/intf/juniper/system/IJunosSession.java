// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.juniper.system;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.net.INetconf;

/**
 * A representation of a JunOS command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IJunosSession extends IBaseSession {
    /**
     * Property indicating the number of milliseconds to wait for a command to begin to return data.
     */
    public static final String PROP_READ_TIMEOUT = "junos.read.timeout";

    /**
     * Get a NETCONF channel to the device.
     */
    INetconf getNetconf();
}
