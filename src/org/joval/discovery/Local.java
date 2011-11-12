// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.discovery;

import org.joval.intf.system.ISession;
import org.joval.os.unix.system.UnixSession;
import org.joval.os.windows.system.WindowsSession;

/**
 * Use this class to grab an IBaseSession for the local machine ONLY.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Local {
    public static ISession createSession() {
	if (System.getProperty("os.name").startsWith("Windows")) {
	    return new WindowsSession();
	} else {
	    return new UnixSession();
	}
    }
}
