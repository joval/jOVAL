// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.discovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.joval.intf.system.ISession;
import org.joval.unix.system.UnixSession;
import org.joval.windows.system.WindowsSession;

/**
 * Use this class to grab an ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Local {
    public static ISession getSession() {
	if (System.getProperty("os.name").startsWith("Windows")) {
	    return new WindowsSession(true);
	} else {
	    return new UnixSession();
	}
    }
}
