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

import org.joval.intf.identity.ICredential;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.unix.remote.system.UnixSession;
import org.joval.os.windows.remote.system.WindowsSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.ssh.system.SshSession;

/**
 * Use this class to grab an ISession for a host.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SessionFactory {
    private static final String HOSTS	= "hosts.xml";
    private static final String SSH	= "ssh";
    private static final String WINDOWS	= "windows";
    private static final String UNKNOWN	= "unknown";

    private Properties props;
    private File propsFile;

    public SessionFactory(File cacheDir) throws IOException {
	props = new Properties();
	propsFile = new File(cacheDir, HOSTS);
	if (propsFile.exists()) {
	    props.loadFromXML(new FileInputStream(propsFile));
	}
    }

    public IBaseSession createSession(String hostname) throws UnknownHostException {
	IBaseSession.Type type = IBaseSession.Type.UNKNOWN;
	String s = props.getProperty(hostname);
	if (s == null) {
	    type = discoverSessionType(hostname);
	} else {
	    type = IBaseSession.Type.getType(s);
	}

	switch (type) {
	  case SSH:
	    return new SshSession(hostname);

	  case WINDOWS:
	    return new WindowsSession(hostname);

	  default:
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSIONTYPE, type));
	}
    }

    // Private

    /**
     * We check for an SSH port listener (22), then an SMB port listener (135).  We check in that order because it is more
     * likely that a Unix machine will be running Samba than a Windows machine will be running an SSH server.
     */
    private IBaseSession.Type discoverSessionType(String hostname) throws UnknownHostException {
	IBaseSession.Type type = IBaseSession.Type.UNKNOWN;
	if (hasListener(hostname, 22)) {
	    type = IBaseSession.Type.SSH;
	} else if (hasListener(hostname, 135)) {
	    type = IBaseSession.Type.WINDOWS;
	} else {
	    type = IBaseSession.Type.UNKNOWN;
	}
	props.setProperty(hostname, type.toString());
	try {
	    props.storeToXML(new FileOutputStream(propsFile), "Session Discovery Database");
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return type;
    }

    private boolean hasListener(String hostname, int port) throws UnknownHostException {
	Socket sock = null;
	try {
	    sock = new Socket(hostname, port);
	    return true;
	} catch (ConnectException e) {
	    return false;
	} catch (IOException e) {
	} finally {
	    if (sock != null) {
		try {
		    sock.close();
		} catch (IOException e) {
		}
	    }
	}
	return false;
    }
}
