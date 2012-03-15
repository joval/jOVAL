// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.cisco.system;

import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.net.INetconf;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.os.cisco.IosSystemInfo;
import org.joval.protocol.netconf.NetconfSession;
import org.joval.ssh.system.SshSession;
import org.joval.util.AbstractBaseSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A simple session implementation for Cisco IOS devices, which is really just an SSH session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosSession extends AbstractBaseSession implements ILocked, IIosSession {
    protected SshSession ssh;
    protected ITechSupport techSupport;
    protected boolean initialized;

    private IosSystemInfo info;

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
    }

    protected void handlePropertyChange(String key, String value) {}

    // Implement IIosSession

    public INetconf getNetconf() {
	return new NetconfSession(ssh, internalProps.getLongProperty(PROP_READ_TIMEOUT));
    }

    public ITechSupport getTechSupport() {
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

    public boolean connect() {
	if (ssh == null) {
	    return false;
	} else if (ssh.connect()) {
	    if (initialized) {
		return true;
	    } else {
		try {
		    techSupport = new TechSupport(this);
		    info = new IosSystemInfo(techSupport);
		    initialized = true;
		    return true;
		} catch (NoSuchElementException e) {
		    logger.warn(JOVALMsg.ERROR_IOS_TECH_SHOW, e.getMessage());
		} catch (Exception e) {
		    logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		return false;
	    }
	} else {
	    return false;
	}
    }

    public void disconnect() {
	ssh.disconnect();
    }

    /**
     * IOS seems to require a session reconnect after every command session disconnect.
     */
    public IProcess createProcess(String command, String[] env) throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_IOS_OFFLINE));
	} else {
//	    disconnect();
	    return ssh.createProcess(command, env);
	}
    }

    public Type getType() {
	return Type.CISCO_IOS;
    }

    public SystemInfoType getSystemInfo() {
	if (info == null) {
	    info = new IosSystemInfo(techSupport);
	}
	return info.getSystemInfo();
    }
}
