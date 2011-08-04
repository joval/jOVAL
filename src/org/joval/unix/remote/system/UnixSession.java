// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix.remote.system;

import java.util.logging.Level;

import org.vngx.jsch.JSch;
import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.UserInfo;
import org.vngx.jsch.exception.JSchException;

import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.unix.Sudo;
import org.joval.unix.UnixFlavor;
import org.joval.unix.UnixSystemInfo;
import org.joval.unix.system.Environment;
import org.joval.unix.remote.UnixCredential;
import org.joval.unix.remote.io.SftpFilesystem;
import org.joval.util.JOVALSystem;

/**
 * A representation of an SSH session, which simply uses JSch to implement an IUnixSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSession implements IUnixSession, ILocked, UserInfo {
    private String hostname;
    private UnixCredential cred;
    private Credential rootCred = null;
    private Session session;
    private IEnvironment env;
    private SftpFilesystem fs;
    private UnixFlavor flavor = UnixFlavor.UNKNOWN;

    public UnixSession(String hostname) {
	this.hostname = hostname;
    }

    // Implement ILocked

    public boolean unlock(ICredential credential) {
	if (credential instanceof UnixCredential) {
	    cred = (UnixCredential)credential;
	    String rootPassword = cred.getRootPassword();
	    if (rootPassword != null) {
		rootCred = new Credential("root", rootPassword);
	    }
	    return true;
	} else {
	    return false;
	}
    }

    // Implement IUnixSession

    public boolean connect() {
	if (cred == null) {
	    return false;
	}
	try {
	    JSch jsch = JSch.getInstance();
	    session = jsch.createSession(cred.getUsername(), hostname);
	    session.setUserInfo(this);
	    session.connect(3000);
	    env = new Environment(this);
	    fs = new SftpFilesystem(session, env);
	    flavor = UnixSystemInfo.getFlavor(this);
	    return true;
	} catch (JSchException e) {
	    e.printStackTrace();
	}
	return false;
    }

    public void disconnect() {
	session.disconnect();
    }

    public void setWorkingDir(String path) {
	// no-op
    }

    public int getType() {
	return UNIX;
    }

    public IFilesystem getFilesystem() {
	return fs;
    }

    public IEnvironment getEnvironment() {
	return env;
    }

    public IProcess createProcess(String command) throws Exception {
	ChannelExec ce = session.openChannel(ChannelType.EXEC);
	IProcess p = new SshProcess(ce, command);
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

    public UnixFlavor getFlavor() {
	return flavor;
    }

    // Implement UserInfo

    public String getPassphrase() {
	return cred.getPassphrase();
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
	JOVALSystem.getLogger().log(Level.INFO, message);
    }
}
