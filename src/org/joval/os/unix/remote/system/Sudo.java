// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.joval.intf.identity.ICredential;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IPrivilegeEscalationDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IPerishable;
import org.joval.io.PerishableReader;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;

/**
 * A tool for running processes as a specific user.  This does not typically actually involve using the sudo command, as
 * it is not normally standard on a Unix operation system.  Rather, it makes use of the su command.  The notable exception
 * is Mac OS X, where it is installed by default (and hence used by this class).
 *
 * It is used exclusively by the remote UnixSession class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Sudo implements IProcess {
    private UnixSession us;
    private String innerCommand;
    private boolean shell = false;
    private IPrivilegeEscalationDriver driver;
    private IProcess p;
    private long timeout;
    private InputStream in=null, err=null;
    private OutputStream out=null;

    Sudo(UnixSession us, String cmd, String[] env) throws Exception {
	this.us = us;
	innerCommand = cmd;
	shell = env != null;
	driver = us.getDriver();
	p = us.ssh.createProcess(driver.getSuString(cmd), env);
	timeout = us.getProperties().getLongProperty(IUnixSession.PROP_SUDO_READ_TIMEOUT);
    }

    // Implement IProcess

    public String getCommand() {
	return innerCommand;
    }

    public void setInteractive(boolean interactive) {
	p.setInteractive(interactive);
    }

    public void start() throws Exception {
	IPrivilegeEscalationDriver.IStreams streams = driver.handleEscalation(p, shell, timeout);
	in = streams.getInputStream();
	err = streams.getErrorStream();
	out = streams.getOutputStream();
    }

    public InputStream getInputStream() throws IOException {
	if (in == null) {
	    in = p.getInputStream();
	}
	return in;
    }

    public InputStream getErrorStream() throws IOException {
	if (err == null) {
	    err = p.getErrorStream();
	}
	return err;
    }

    public OutputStream getOutputStream() throws IOException {
	if (out == null) {
	    out = p.getOutputStream();
	}
	return out;
    }

    public void waitFor(long millis) throws InterruptedException {
	p.waitFor(millis);
    }

    public int exitValue() throws IllegalThreadStateException {
	return p.exitValue();
    }

    public void destroy() {
	p.destroy();
    }

    public boolean isRunning() {
	return p.isRunning();
    }
}
