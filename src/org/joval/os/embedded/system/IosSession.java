// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.embedded.system;

import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.os.embedded.IosSystemInfo;
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
    private SshSession ssh;
    private TechSupport techSupport;
    private IosSystemInfo info;
    private boolean initialized, offline;

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
    public IosSession(TechSupport techSupport) {
	super();
	this.techSupport = techSupport;
	info = new IosSystemInfo(techSupport);
    }

    protected void handlePropertyChange(String key, String value) {}

    // Implement IIosSession

    public ITechSupport getTechSupport() {
	return techSupport;
    }

    // Implement ILocked

    public boolean unlock(ICredential cred) {
	return ssh.unlock(cred);
    }

    // Implement ILogger

    /**
     * @override
     */
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	ssh.setLogger(logger);
    }

    // Implement IBaseSession

    public String getHostname() {
	return ssh.getHostname();
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
    public IProcess createProcess(String command) throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_IOS_OFFLINE));
	} else {
	    disconnect();
	    return ssh.createProcess(command);
	}
    }

    public Type getType() {
	return Type.CISCO_IOS;
    }

    public SystemInfoType getSystemInfo() {
	return info.getSystemInfo();
    }
}
