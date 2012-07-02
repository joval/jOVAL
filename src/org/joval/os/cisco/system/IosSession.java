// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.cisco.system;

import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import org.w3c.dom.Document;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.ssh.system.IShell;
import org.joval.intf.ssh.system.IShellProcess;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.protocol.netconf.NetconfSession;
import org.joval.ssh.system.SshProcess;
import org.joval.ssh.system.SshSession;
import org.joval.util.AbstractBaseSession;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * A simple session implementation for Cisco IOS devices, which is really just an SSH session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosSession extends AbstractBaseSession implements ILocked, IIosSession {
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS z EEE MMM dd yyyy");
    private SshSession ssh;
    private IShellProcess shell = null;
    private ITechSupport techSupport;
    private Document config;
    private boolean initialized;

    /**
     * Create an IOS session with a live SSH connection to a router.
     */
    public IosSession(SshSession ssh) {
	super();
	this.ssh = ssh;
	initialized = false;
    }

    /**
     * Create an IOS session in offline mode, using the supplied tech support information.
     */
    public IosSession(ITechSupport techSupport) {
	super();
	this.techSupport = techSupport;
	initialized = true;
    }

    // Implement INetconf

    public Document getConfig() throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_OFFLINE));
	} else if (config == null) {
	    //
	    // A Cisco SSH session only permits one channel at a time, so close the shell if it's open.
	    //
	    if (shell != null) {
		shell.setKeepAlive(false);
		shell.getShell().close();
		shell = null;

		// In all likelihood, closing the shell channel will cause the connection to be dropped, so
		// just disconnect cleanly to get it over with.
		ssh.disconnect();
	    }

	    NetconfSession netconf = new NetconfSession(ssh, internalProps.getLongProperty(PROP_READ_TIMEOUT));
	    netconf.setLogger(logger);
	    config = netconf.getConfig();

	    //
	    // Closing the NETCONF subsystem channel will probably cause the connection to be dropped, so
	    // we disconnect cleanly to get it over with.
	    //
	    ssh.disconnect();
	}
	return config;
    }

    // Implement IIosSession

    public ITechSupport getTechSupport() {
	if (!initialized) {
	    try {
		techSupport = new TechSupport(this);
		initialized = true;
	    } catch (Exception e) {
		logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return techSupport;
    }

    public IShell getShell() throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_OFFLINE));
	}
	if (isConnected() || connect()) {
	    return shell.getShell();
	} else {
	    throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_CONNECT));
	}
    }

    // Implement ILocked

    public boolean unlock(ICredential cred) {
	return ssh.unlock(cred);
    }

    // Implement ILogger

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (ssh != null) {
	    ssh.setLogger(logger);
	}
    }

    // Implement IBaseSession

    @Override
    public String getUsername() {
	if (ssh == null) {
	    return null;
	} else {
	    return ssh.getUsername();
	}
    }

    public String getHostname() {
	if (ssh != null) {
	    return ssh.getHostname();
	} else {
	    for (String line : techSupport.getLines("show running-config")) {
		if (line.startsWith("hostname ")) {
		    return line.substring(9).trim();
		}
	    }
	    return null;
	}
    }

    @Override
    public boolean isConnected() {
	if (ssh != null) {
	    return ssh.isConnected();
	} else if (techSupport != null) {
	    return connected; // offline mode
	} else {
	    return false;
	}
    }

    public boolean connect() {
	if (ssh == null) {
	    return (connected = initialized);
	} else {
	    if (ssh.connect()) {
		try {
		    shellInit();
		    return true;
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return false;
    }

    public void disconnect() {
	if (ssh != null) {
	    ssh.disconnect();
	    shell = null;
	}
	connected = false;
    }

    public long getTime() throws Exception {
	long to = getProperties().getLongProperty(PROP_READ_TIMEOUT);
	for (String line : SafeCLI.multiLine("show clock", this, to)) {
	    if (line.startsWith("*")) {
		synchronized(sdf) {
		    return sdf.parse(line.substring(1)).getTime();
		}
	    }
	}
	throw new NoSuchElementException("*");
    }

    /**
     * Recycle the same shell over and over.
     */
    public IProcess createProcess(String command, String[] env) throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_OFFLINE));
	}
	if (isConnected() || connect()) {
	    if (shell.isAlive()) {
		return shell.newProcess(command);
	    } else {
		shellInit();
		return shell;
	    }
	} else {
	    throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_CONNECT));
	}
    }

    public Type getType() {
	return Type.CISCO_IOS;
    }

    // Private

    private void shellInit() throws Exception {
	shell = (IShellProcess)ssh.createSshProcess("terminal length 0", null, SshProcess.Type.SHELL);
	shell.setKeepAlive(true);
	shell.start();
	while(shell.getInputStream().read() != -1) {}
	shell.getInputStream().close();
    }
}
