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
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.security.auth.login.LoginException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ICredentialStore;
import org.joval.intf.identity.ILocked;
import org.joval.intf.util.ILoggable;
import org.joval.intf.system.IBaseSession;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.unix.remote.system.UnixSession;
import org.joval.os.windows.remote.system.WindowsSession;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Use this class to grab an ISession for a host.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SessionFactory implements ILoggable {
    private static final String PROPS		= "host.properties";
    private static final String PROP_SESSION	= "session.type";

    private File wsDir = null;
    private ICredentialStore cs;
    private Hashtable<String, String> routes;
    private LocLogger logger;

    /**
     * Create a SessionFactory with no state persistence capability.
     */
    public SessionFactory() {
	routes = new Hashtable<String, String>();
	logger = JOVALSystem.getLogger();
    }

    public SessionFactory(File wsDir) throws IOException {
	this();
	setDataDirectory(wsDir);
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

    public void setCredentialStore(ICredentialStore cs) {
	this.cs = cs;
    }

    public void addRoute(String destination, String gateway) {
	routes.put(destination, gateway);
    }

    /**
     * Given the hostname, return an authenticated session.  The session may or may not be connected.
     */
    public IBaseSession createSession(String hostname) throws UnknownHostException, ConnectException {
	if (hostname == null) {
	    throw new ConnectException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_TARGET));
	}

	File dir = getHostWorkspace(hostname);

	IBaseSession.Type type = IBaseSession.Type.UNKNOWN;
	List<String> route = getRoute(hostname);
	if (route.size() == 0) {
	    //
	    // Look up or discover the session type (Windows or SSH)
	    //
	    Properties props = getProperties(hostname);
	    String s = props.getProperty(PROP_SESSION);
	    if (s == null) {
		type = discoverSessionType(hostname);
	    } else {
		type = IBaseSession.Type.typeOf(s);
	    }
	} else {
	    //
	    // Only SSH sessions support routes
	    //
	    type = IBaseSession.Type.SSH;
	}

	//
	// Generate the appropriate session implementation (Windows, Cisco IOS or Unix)
	//
	try {
	    IBaseSession session = null;
	    switch (type) {
	      case SSH:
		SshSession gateway = null;
		for (String next : route) {
		    gateway = new SshSession(next, gateway, dir);
		    gateway.setLogger(logger);
		    setCredential(gateway);
		}
		SshSession ssh = new SshSession(hostname, gateway, dir);
		ssh.setLogger(logger);
		setCredential(ssh);
		switch(ssh.getType()) {
		  case UNIX:
		    session = new UnixSession(ssh);
		    break;

		  case CISCO_IOS:
		    ssh.disconnect();
		    session = new IosSession(ssh);
		    break;

		  default:
		    session = ssh;
		}
		break;

	      case WINDOWS:
		session = new WindowsSession(hostname, dir);
		break;

	      default:
		throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_TYPE, type));
	    }
	    session.setLogger(logger);
	    setCredential(session);
	    return session;
	} catch (Exception e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new ConnectException(e.getMessage());
	}
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
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
	props.setProperty(PROP_SESSION, type.value());
	saveProperties(props, hostname);
	return type;
    }

    private void setCredential(IBaseSession session) throws Exception {
	if (session instanceof ILocked) {
	    if (cs == null) {
		throw new Exception(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_CREDENTIAL_STORE, session.getHostname()));
	    } else {
		ICredential cred = cs.getCredential(session);
		if (cred == null) {
		    throw new LoginException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_CREDENTIAL));
		} else if (((ILocked)session).unlock(cred)) {
		    JOVALSystem.getLogger().debug(JOVALMsg.STATUS_CREDENTIAL_SET, session.getHostname());
		} else {
		    String baseName = session.getClass().getName();
		    String credName = cred.getClass().getName();
		    throw new Exception(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_LOCK, credName, baseName));
		}
	    }
	}
    }

    /**
     * Returns a list of gateways that must be traversed (in the order they must be contacted) to reach the indicated
     * host.  If the host can be contacted directly (i.e., no route is defined), the list will be empty.
     */
    private List<String> getRoute(String hostname) {
	List<String> route = new Vector<String>();
	String gateway = hostname;
	while((gateway = routes.get(gateway)) != null) {
	    route.add(0, gateway);
	}
	return route;
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
