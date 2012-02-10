// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.IOException;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.JSch;
import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.UIKeyboardInteractive;
import org.vngx.jsch.UserInfo;
import org.vngx.jsch.config.SSHConfigConstants;
import org.vngx.jsch.config.SessionConfig;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.userauth.Identity;
import org.vngx.jsch.userauth.IdentityManager;
import org.vngx.jsch.userauth.IdentityFile;
import org.vngx.jsch.util.SocketFactory;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.ssh.identity.ISshCredential;
import org.joval.intf.ssh.system.ISshSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProperty;
import org.joval.util.AbstractBaseSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.JSchLogger;
import org.joval.util.PropertyUtil;
import org.joval.util.SafeCLI;

/**
 * A representation of an SSH session, which simply uses JSch to implement an IBaseSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SshSession extends AbstractBaseSession implements ISshSession, ILocked, UserInfo, UIKeyboardInteractive {
    private String hostname;
    private ICredential cred;
    private SshSession gateway;
    private int gatewayPort = 0;
    private Session session;
    private boolean connected = false;
    private int pid = 0; // internal process ID

    public SshSession(String hostname) {
	this(hostname, null, null);
    }

    public SshSession(String hostname, File wsdir) {
	this(hostname, null, wsdir);
    }

    public SshSession(String hostname, SshSession gateway, File wsdir) {
	super();
	this.wsdir = wsdir;
	this.hostname = hostname;
	this.gateway = gateway;
    }

    public Session getJschSession() {
	return session;
    }

    protected void handlePropertyChange(String key, String value) {
	super.handlePropertyChange(key, value);
	if (PROP_ATTACH_LOG.equals(key)) {
	    if (internalProps.getBooleanProperty(key)) {
		JSch.setLogger(new JSchLogger(JOVALSystem.getLogger()));
	    } else {
		JSch.setLogger(null);
	    }
	}
    }

    // Implement ILocked

    public boolean unlock(ICredential cred) {
	if (cred == null) {
	    logger.warn(JOVALMsg.ERROR_SESSION_CREDENTIAL);
	    return false;
	}
	this.cred = cred;
	if (cred instanceof ISshCredential) {
	    ISshCredential sshc = (ISshCredential)cred;
	    if (sshc.getPrivateKey() != null) {
		try {
		    Identity id = IdentityFile.newInstance(sshc.getPrivateKey().getPath(), null);
		    if (sshc.getPassphrase() == null) {
			IdentityManager.getManager().addIdentity(id, null);
		    } else {
			IdentityManager.getManager().addIdentity(id, sshc.getPassphrase().getBytes());
 		    }
		} catch (JSchException e) {
		    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return true;
    }

    // Implement IBaseSession

    /**
     * @override
     */
    public IProcess createProcess(String command) throws Exception {
	if (connect()) {
	    ChannelExec ce = session.openChannel(ChannelType.EXEC);
	    return new SshProcess(ce, command, getDebug(), wsdir, pid++, logger);
	} else {
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_SSH_DISCONNECTED));
	}
    }

    public String getHostname() {
	return hostname;
    }

    public boolean connect() {
	if (gateway != null && !gateway.connect()) {
	    return false;
	}

	if (cred == null) {
	    return false;
	} else if (session == null) {
	    try {
		connectInternal();
		return true;
	    } catch (JSchException e) {
		logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	if (session.isConnected()) {
	    return true; // already connected
	} else {
	    if (connected) {
		logger.warn(JOVALMsg.ERROR_SSH_DROPPED_CONN, hostname);
	    }
	    Exception lastError = null;
	    int retries = internalProps.getIntProperty(PROP_CONNECTION_RETRIES);
	    for (int i=1; i <= retries; i++) {
		try {
		    connectInternal();
		    return true;
		} catch (JSchException e) {
		    lastError = e;
		    logger.warn(JOVALMsg.ERROR_SSH_CONNECT, hostname, i, e.getMessage());
		}
	    }
	    if (lastError != null) {
		logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), lastError);
	    }
	    return false;
	}
    }

    public void disconnect() {
	if (session != null) {
	    synchronized(session) {
	        if (session != null && connected) {
		    logger.info(JOVALMsg.STATUS_SSH_DISCONNECT, hostname);
		    session.disconnect();
		    connected = false;
		}
	    }
	}
	if (gateway != null) {
	    if (gatewayPort > 0) {
		try {
		    gateway.session.delPortForwardingL(gatewayPort);
		} catch (JSchException e) {
		}
		gatewayPort = 0;
	    }
	    gateway.disconnect();
	}
    }

    public Type getType() {
	if (cred != null) {
	    try {
		for (String line : SafeCLI.multiLine("pwd", this, Timeout.S)) {
		    if (line.startsWith("/")) {
			return Type.UNIX;
		    } else if (line.startsWith("flash")) {
			return Type.CISCO_IOS;
		    }
		}
		for (String line : SafeCLI.multiLine("show version", this, Timeout.S)) {
		    if (line.startsWith("JUNOS")) {
			return Type.JUNIPER_JUNOS;
		    } else if (line.startsWith("Cisco IOS")) {
			return Type.CISCO_IOS;
		    }
		}
	    } catch (Exception e) {
		logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return Type.SSH;
    }

    public SystemInfoType getSystemInfo() {
	return null;
    }

    public boolean getDebug() {
	return internalProps.getBooleanProperty(PROP_DEBUG);
    }

    // Implement UserInfo

    public String getPassphrase() {
	if (cred instanceof ISshCredential) {
	    return ((ISshCredential)cred).getPassphrase();
	} else {
	    return "";
	}
    }

    public String getPassword() {
	return cred.getPassword();
    }

    public boolean promptPassphrase(String message) {
	return true;
    }

    public boolean promptPassword(String message) {
	return true;
    }

    public boolean promptYesNo(String message) {
	return true;
    }

    public void showMessage(String message) {
	logger.debug(JOVALMsg.STATUS_AUTHMESSAGE, message);
    }

    // Implement UIKeyboardInteractive

    /**
     * Mac OS X allows private key and interactive logins only by default, so this interface must be implemented.
     */
    public String[] promptKeyboardInteractive(String dest, String name, String instruction, String[] prompt, boolean[] echo) {
	return null;
    }

    // Private

    private void connectInternal() throws JSchException {
	SessionConfig config = new SessionConfig();
	config.setProperty(SSHConfigConstants.STRICT_HOST_KEY_CHECKING, "no");
	JSch jsch = JSch.getInstance();
	if (gateway == null) {
	    if (session == null) {
		session = jsch.createSession(cred.getUsername(), hostname, 22, config);
		session.setUserInfo(this);
	    }
	} else {
	    // First, disconnect if applicable
	    if (session != null) {
		session.disconnect();
		session = null;
	    }
	    if (gatewayPort > 0) {
		try {
		    gateway.session.delPortForwardingL(gatewayPort);
		} catch (JSchException e) {
		}
		gatewayPort = 0;
	    }

	    // Then, use gateway port forwarding
	    gatewayPort = gateway.session.setPortForwardingL(0, hostname, 22);
	    session = jsch.createSession(cred.getUsername(), LOCALHOST, gatewayPort, config);
	    session.setUserInfo(this);
	}
	session.setSocketFactory(SocketFactory.DEFAULT_SOCKET_FACTORY);
	session.connect(internalProps.getIntProperty(PROP_CONNECTION_TIMEOUT));
	connected = true;
	logger.info(JOVALMsg.STATUS_SSH_CONNECT, hostname);
    }
}
