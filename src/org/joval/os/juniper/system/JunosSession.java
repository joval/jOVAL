// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.juniper.system;

import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.juniper.system.ISupportInformation;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.net.INetconf;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.os.cisco.system.IosSession;
import org.joval.protocol.netconf.NetconfSession;
import org.joval.ssh.system.SshSession;
import org.joval.util.AbstractBaseSession;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * A simple session implementation for Juniper JunOS devices, which is really very similar to an IOS session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JunosSession extends AbstractBaseSession implements ILocked, IJunosSession {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private SshSession ssh;
    private ISupportInformation supportInfo;
    private boolean initialized;

    /**
     * Create an IOS session with a live SSH connection to a router.
     */
    public JunosSession(SshSession ssh) {
	super();
	this.ssh = ssh;
	initialized = false;
    }

    public JunosSession(ISupportInformation supportInfo) {
	super();
	this.supportInfo = supportInfo;
	initialized = true;
    }

    // Implement IJunosSession

    public ISupportInformation getSupportInformation() {
	if (!initialized) {
	    try {
		supportInfo = new SupportInformation(this);
		initialized = true;
	    } catch (Exception e) {
		logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return supportInfo;
    }

    public INetconf getNetconf() {
	return new NetconfSession(ssh, internalProps.getLongProperty(PROP_READ_TIMEOUT));
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
	    for (String line : supportInfo.getData("show version detail")) {
		if (line.startsWith("Hostname: ")) {
		    return line.substring(10).trim();
		}
	    }
	    return null;
	}
    }

    public long getTime() throws Exception {
	long to = getProperties().getLongProperty(PROP_READ_TIMEOUT);
	for (String line : SafeCLI.multiLine("show system uptime", this, to)) {
	    if (line.startsWith("Current time: ")) {
		synchronized(sdf) {
		    return sdf.parse(line.substring(14)).getTime();
		}
	    }
	}
	throw new NoSuchElementException("Current time");
    }

    @Override
    public boolean isConnected() {
	if (ssh != null) {
	    return ssh.isConnected();
	} else if (supportInfo != null) {
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

    public IProcess createProcess(String command, String[] env) throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_JUNOS_OFFLINE));
	} else {
	    return ssh.createProcess(command, env);
	}
    }

    public Type getType() {
	return Type.JUNIPER_JUNOS;
    }
}
