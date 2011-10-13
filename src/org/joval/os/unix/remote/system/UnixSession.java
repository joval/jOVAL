// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.Sudo;
import org.joval.os.unix.UnixSystemInfo;
import org.joval.os.unix.system.Environment;
import org.joval.ssh.identity.SshCredential;
import org.joval.ssh.io.SftpFilesystem;
import org.joval.ssh.system.SshSession;
import org.joval.util.BaseSession;

/**
 * A representation of Unix session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSession extends BaseSession implements ILocked, IUnixSession {
    private SshSession ssh;
    private ICredential cred;
    private Credential rootCred = null;
    private Flavor flavor = Flavor.UNKNOWN;
    private UnixSystemInfo info = null;

    public UnixSession(SshSession ssh) {
	this.ssh = ssh;
	info = new UnixSystemInfo(this);
    }

    // Implement ILocked

    public boolean unlock(ICredential cred) {
	if (cred instanceof SshCredential) {
	    String rootPassword = ((SshCredential)cred).getRootPassword();
	    if (rootPassword != null) {
		rootCred = new Credential("root", rootPassword);
	    }
	}
	this.cred = cred;
	return ssh.unlock(cred);
    }

    // Implement IBaseSession

    public boolean connect() {
	if (ssh.connect()) {
	    env = new Environment(this);
	    fs = new SftpFilesystem(ssh.getJschSession(), this, env);
	    flavor = Flavor.flavorOf(this);
	    return fs.connect();
	} else {
	    return false;
	}
    }

    public void disconnect() {
	fs.disconnect();
	ssh.disconnect();
    }

    /**
     * @override
     */
    public IProcess createProcess(String command) throws Exception {
	return createProcess(command, 3600000L, false);
    }

    public Type getType() {
	return Type.UNIX;
    }

    // Implement ISession

    public SystemInfoType getSystemInfo() {
	return info.getSystemInfo();
    }

    /**
     * @override
     */
    public void setWorkingDir(String path) {
	// no-op
    }

    // Implement IUnixSession

    public Flavor getFlavor() {
	return flavor;
    }

    public IProcess createProcess(String command, long millis, boolean debug) throws Exception {
	IProcess p = ssh.createProcess(command, millis, debug);
	switch(flavor) {
	  case LINUX:
	  case SOLARIS:
	    if (rootCred != null) {
		p = new Sudo(p, flavor, rootCred);
	    }
	    // fall-through

	  default:
	    return p;
	}
    }
}
