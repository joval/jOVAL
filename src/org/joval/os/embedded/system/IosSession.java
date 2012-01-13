// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.embedded.system;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.os.embedded.IosSystemInfo;
import org.joval.ssh.system.SshSession;
import org.joval.util.BaseSession;

/**
 * A simple session implementation for Cisco IOS devices, which is really just an SSH session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosSession extends BaseSession implements ILocked, ISession {
    private SshSession ssh;
    private IosSystemInfo info;

    public IosSession(SshSession ssh) {
	super();
	this.ssh = ssh;
	info = new IosSystemInfo(this);
    }

    // Implement ILocked

    public boolean unlock(ICredential cred) {
	return ssh.unlock(cred);
    }

    // Implement IBaseSession

    public String getHostname() {
	return ssh.getHostname();
    }

    public boolean connect() {
	if (ssh.connect()) {
	    info.getSystemInfo();
	    return true;
	}
	return false;
    }

    public void disconnect() {
	ssh.disconnect();
    }

    /**
     * IOS seems to require a session reconnect after every command session disconnect.
     */
    public IProcess createProcess(String command) throws Exception {
	disconnect();
	return ssh.createProcess(command);
    }

    public Type getType() {
	return Type.CISCO_IOS;
    }

    /**
     * @override
     */
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	ssh.setLogger(logger);
    }

    // Implement ISession

    public SystemInfoType getSystemInfo() {
	return info.getSystemInfo();
    }

    /**
     * @override
     */
    public void setWorkingDir(String dir) {
	// no-op
    }

    /**
     * @override
     */
    public IFilesystem getFilesystem() {
	return null;
    }

    /**
     * @override
     */
    public IEnvironment getEnvironment() {
	return null;
    }
}
