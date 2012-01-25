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
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IPerishable;
import org.joval.io.PerishableReader;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A tool for running processes as a specific user.  This does not use the sudo command, rather, it makes use of the
 * su command.  It is used exclusively by the remote UnixSession class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Sudo implements IProcess {
    private SshSession ssh;
    private UnixSession us;
    private IProcess p;
    private ICredential cred;
    private String innerCommand;
    private PerishableReader in=null, err=null;
    private OutputStream out=null;

    Sudo(UnixSession us, ICredential cred, String cmd) throws Exception {
	this.us = us;
	ssh = us.ssh;
	this.cred = cred;
	innerCommand = cmd;
	p = ssh.createProcess(getSuString(cmd));
    }

    // Implement IProcess

    public void setInteractive(boolean interactive) {
	p.setInteractive(interactive);
    }

    public void start() throws Exception {
	if (cred.getPassword() == null) {
	    throw new CredentialException(JOVALSystem.getMessage(JOVALMsg.ERROR_MISSING_PASSWORD, cred.getUsername()));
	}

	switch(us.getFlavor()) {
	  case SOLARIS:
	    loginSolaris();
	    break;

	  default:
	    loginDefault();
	    break;
	}
    }

    public InputStream getInputStream() throws IOException {
	if (in == null) {
	    long readTimeout = us.getProperties().getLongProperty(IUnixSession.PROP_READ_TIMEOUT);
	    in = (PerishableReader)PerishableReader.newInstance(p.getInputStream(), readTimeout);
	    in.setLogger(us.getLogger());
	}
	return in;
    }

    public InputStream getErrorStream() throws IOException {
	if (err == null) {
	    long readTimeout = us.getProperties().getLongProperty(IUnixSession.PROP_SUDO_READ_TIMEOUT);
	    err = (PerishableReader)PerishableReader.newInstance(p.getErrorStream(), readTimeout);
	    err.setLogger(us.getLogger());
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

    // Private

    /**
     * Perform a normal login, by simply writing the root password to the su process standard input.
     */
    private void loginDefault() throws Exception {
	p.start();
	getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write('\n');
	out.flush();
    }

    /**
     * Perform an interactive login on Solaris, and skip past the Message Of The Day (if any).
     */
    private void loginSolaris() throws Exception {
	setInteractive(true);
	p.start();
	getInputStream();
	byte[] buff = new byte[10]; //Password:_
	in.readFully(buff);
	getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write('\r');
	out.flush();
	String line1 = in.readLine();
	if (line1 == null) {
	    throw new EOFException(null);
	} else if (line1.indexOf("Sorry") == -1) {
	    //
	    // Skip past the message of the day
	    //
	    int linesToSkip = us.getMotdLines();
	    for (int i=0; i < linesToSkip; i++) {
		if (in.readLine() == null) {
		    throw new EOFException(null);
		}
	    }
	} else {
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_AUTHENTICATION_FAILED, cred.getUsername());
	    throw new LoginException(msg);
	}
    }

    private String getSuString(String command) {
	StringBuffer sb = new StringBuffer("su - ");
	sb.append(cred.getUsername());
	sb.append(" -c \"");
	sb.append(command.replace("\"", "\\\""));
	sb.append("\"");
	return sb.toString();
    }
}
