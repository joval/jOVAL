// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.juniper.system;

import org.joval.intf.cisco.system.IIosSession;

/**
 * A representation of a JunOS command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IJunosSession extends IIosSession {
    /**
     * Property indicating the number of milliseconds to wait for a command to begin to return data.
     *
     * NOTE: This overloads the definition of PROP_READ_TIMEOUT inherited from IIosSession.
     */
    String PROP_READ_TIMEOUT = "junos.read.timeout";
}
