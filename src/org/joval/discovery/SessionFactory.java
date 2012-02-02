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

import org.joval.intf.system.IBaseSession;
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
    private static final String ROUTES	= "routes.ini";
    private static final String PROPS	= "host.properties";

    private File wsDir = null;
    private SshSession gateway = null;

    /**
     * Create a SessionFactory with no state persistence capability.
     */
    public SessionFactory() {
    }

    public SessionFactory(File wsDir) throws IOException {
	setDataDirectory(wsDir);
    }

    public SessionFactory(File wsDir, SshSession gateway) throws IOException {
	setDataDirectory(wsDir);
	setSshGateway(gateway);
    }

    public void setDataDirectory(File wsDir) throws IOException {
	this.wsDir = wsDir;
	if (!wsDir.exists()) {
	    wsDir.mkdirs();
	}
	if (!wsDir.isDirectory()) {
	    throw new IOException(JOVALSystem.getMessage(JOVALMsg.ERROR_DIRECTORY, wsDir.getPath()));
	}
    }

    public void setSshGateway(SshSession gateway) {
	this.gateway = gateway;
    }

    public IBaseSession createSession(String hostname) throws UnknownHostException {
	File dir = getHostWorkspace(hostname);

	if (gateway != null) {
	    return new SshSession(hostname, gateway, dir);
	}

	Properties props = getProperties(hostname);
	IBaseSession.Type type = IBaseSession.Type.UNKNOWN;
	String s = props.getProperty(hostname);
	if (s == null) {
	    type = discoverSessionType(hostname);
	} else {
	    type = IBaseSession.Type.getType(s);
	}

	switch (type) {
	  case SSH:
	    return new SshSession(hostname, dir);

	  case WINDOWS:
	    return new WindowsSession(hostname, dir);

	  default:
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_TYPE, type));
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

	Properties props = getProperties(hostname);
	props.setProperty(hostname, type.toString());
	saveProperties(props, hostname);
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

    private File getHostWorkspace(String hostname) {
	File hostDir = null;
	if (wsDir != null) {
	    hostDir = new File(wsDir, hostname);
	    if (!hostDir.exists()) {
		hostDir.mkdir();
	    }
	}
	return hostDir;
    }

    private Properties getProperties(String hostname) {
	Properties props = new Properties();
	File hostDir = getHostWorkspace(hostname);
	if (hostDir != null) {
	    File hostProps = new File(hostDir, PROPS);
	    if (hostProps.isFile()) {
		try {
		    props.load(new FileInputStream(hostProps));
		} catch (IOException e) {
		    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_IO, hostProps, e.getMessage());
		}
	    }
	}
	return props;
    }

    private void saveProperties(Properties props, String hostname) {
	File hostDir = getHostWorkspace(hostname);
	if (hostDir != null) {
	    File hostProps = new File(hostDir, PROPS);
	    try {
		props.store(new FileOutputStream(hostProps), "Session Discovery Data");
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
