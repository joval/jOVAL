// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.cisco.system;

import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.net.INetconf;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.protocol.netconf.NetconfSession;
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
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS z EEE MMM dd yyyy");

    private SshSession ssh;
    private ITechSupport techSupport;
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

    protected void handlePropertyChange(String key, String value) {}

    // Implement IIosSession

    public INetconf getNetconf() {
	return new NetconfSession(ssh, internalProps.getLongProperty(PROP_READ_TIMEOUT));
    }

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

    public String getHostname() {
	if (ssh != null) {
	    return ssh.getHostname();
	} else {
	    for (String line : techSupport.getData("show running-config")) {
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
	    return ssh.connect();
	}
    }

    public void disconnect() {
	if (ssh != null) {
	    ssh.disconnect();
	}
	connected = false;
    }

    public long getTime() throws Exception {
	long to = getProperties().getLongProperty(PROP_READ_TIMEOUT);
	for (String line : SafeCLI.multiLine("show clock", this, to)) {
	    if (line.startsWith("*")) {
		return SDF.parse(line.substring(1)).getTime();
	    }
	}
	throw new NoSuchElementException("*");
    }

    /**
     * IOS seems to require a session reconnect after every command session disconnect.
     */
    public IProcess createProcess(String command, String[] env) throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_OFFLINE));
	} else {
//	    disconnect();
	    return ssh.createProcess(command, env);
	}
    }

    public Type getType() {
	return Type.CISCO_IOS;
    }
}
