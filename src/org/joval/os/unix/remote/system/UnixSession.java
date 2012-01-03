// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFile;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.PerishableReader;
import org.joval.os.unix.UnixSystemInfo;
import org.joval.os.unix.system.Environment;
import org.joval.ssh.identity.SshCredential;
import org.joval.ssh.io.SftpFilesystem;
import org.joval.ssh.system.SshSession;
import org.joval.util.BaseSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A representation of Unix session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSession extends BaseSession implements ILocked, IUnixSession {
    private static final String MOTD = "/etc/motd";

    SshSession ssh;

    private LocLogger logger;
    private ICredential cred;
    private Credential rootCred = null;
    private Flavor flavor = Flavor.UNKNOWN;
    private UnixSystemInfo info = null;
    private boolean computedMotdLines = false;
    private int motdLines = 0;

    public UnixSession(SshSession ssh) {
	this.ssh = ssh;
	info = new UnixSystemInfo(this);
	logger = JOVALSystem.getLogger();
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

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IBaseSession

    public void setDebug(boolean debug) {
	ssh.setDebug(debug);
    }

    public String getHostname() {
	return ssh.getHostname();
    }

    public boolean connect() {
	if (ssh.connect()) {
	    if (env == null) {
		env = new Environment(this);
	    }
	    if (fs == null) {
		fs = new SftpFilesystem(ssh.getJschSession(), this, env);
	    } else {
		((SftpFilesystem)fs).setJschSession(ssh.getJschSession());
	    }
	    if (flavor == Flavor.UNKNOWN) {
		flavor = Flavor.flavorOf(this);
	    }
	    info.getSystemInfo();
	    return true;
	} else {
	    return false;
	}
    }

    public void disconnect() {
	if (fs != null) {
	    fs.disconnect();
	}
	if (ssh != null) {
	    ssh.disconnect();
	}
    }

    /**
     * @override
     */
    public IProcess createProcess(String command) throws Exception {
	switch(flavor) {
	  case LINUX:
	  case SOLARIS:
	    if (rootCred != null) {
		return new Sudo(this, rootCred, command);
	    }
	}
	return ssh.createProcess(command);
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

    // Internal

    /**
     * Returns the number of lines in the Message of the Day file.  This allows an interactive session to learn how many
     * lines of input it should skip (i.e., the Sudo class running on Solaris).
     */
    int getMotdLines() {
	if (!computedMotdLines) {
	    try {
		IFile motd = fs.getFile(MOTD);
		if (motd.exists()) {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(motd.getInputStream()));
		    String line = null;
		    while ((line = reader.readLine()) != null) {
			motdLines++;
		    }
		    reader.close();
		}
		computedMotdLines = true;
	    } catch (IOException e) {
		logger.warn(JOVALMsg.ERROR_IO, MOTD, e.getMessage());
	    }
	}
	return motdLines;
    }
}
