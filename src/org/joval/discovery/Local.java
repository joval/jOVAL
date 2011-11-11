// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.discovery;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import org.joval.intf.discovery.ISessionFactory;
import org.joval.intf.system.IBaseSession;
import org.joval.os.unix.system.UnixSession;
import org.joval.os.windows.system.WindowsSession;

/**
 * Use this class to grab an IBaseSession for the local machine ONLY.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Local implements ISessionFactory {
    public Local() {}

    // Implement ISessionFactory

    public void setDataDirectory(File dir) throws IOException {
    }

    public IBaseSession createSession(String hostname) throws UnknownHostException {
	if (LOCALHOST.equalsIgnoreCase(hostname)) {
	    if (System.getProperty("os.name").startsWith("Windows")) {
		return new WindowsSession();
	    } else {
		return new UnixSession();
	    }
	} else {
	    throw new UnknownHostException(hostname);
	}
    }
}
