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
    static final int CR = 0x0D;
    static final int LF = 0x0A;

    private SshSession ssh;
    private UnixSession us;
    private IProcess p;
    private ICredential cred;
    private String innerCommand;
    private PerishableReader in=null, err=null;
    private OutputStream out=null;
    private boolean shell = false;
    private long timeout;

    Sudo(UnixSession us, ICredential cred, String cmd, String[] env) throws Exception {
	this.us = us;
	ssh = us.ssh;
	this.cred = cred;
	innerCommand = cmd;
	shell = env != null;
	p = ssh.createProcess(getSuString(cmd), env);
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
	if (cred.getPassword() == null) {
	    throw new CredentialException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_PASSWORD, cred.getUsername()));
	}

	switch(us.getFlavor()) {
	  case AIX:
	    loginAIX();
	    break;

	  case LINUX:
	    loginLinux();
	    break;

	  case MACOSX:
	    sudoMacOSX();
	    break;

	  case SOLARIS:
	    loginSolaris();
	    break;

	  default:
	    loginLinux();
	    break;
	}
    }

    public InputStream getInputStream() throws IOException {
	if (in == null) {
	    in = (PerishableReader)PerishableReader.newInstance(p.getInputStream(), timeout);
	    in.setLogger(us.getLogger());
	}
	return in;
    }

    public InputStream getErrorStream() throws IOException {
	if (err == null) {
	    err = (PerishableReader)PerishableReader.newInstance(p.getErrorStream(), timeout);
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
     * Perform a normal login by reading the prompt from the error stream, and entering LF after the password.
     */
    private void loginLinux() throws Exception {
	p.start();
	if (shell) {
	    ((PerishableReader)getInputStream()).readFully(new byte[10]); // Password:_
	} else {
	    ((PerishableReader)getErrorStream()).readFully(new byte[10]); // Password:_
	}
	getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write(shell ? CR : LF);
	out.flush();
    }

    /**
     * On Mac OSX, perform an interactive login by reading the prompt from the input stream, and entering CR after the
     * 9-character password prompt.
     *
     * Note that root is not normally enabled, and when it is, you're not allowed to su to root from a remote terminal.
     */
    private void sudoMacOSX() throws Exception {
	setInteractive(true);
	p.start();
	getInputStream();
	in.setCheckpoint(512);
	boolean success = false;
	for (int i=0; i < 512 && !success; i++) {
	    int ch = in.read();
	    if ('?' == ch) {
		getOutputStream();
		out.write(us.getSessionCredential().getPassword().getBytes());
		out.write(LF);
		out.flush();
		success = true;
	    }
	}
	if (!success) {
	    in.restoreCheckpoint();
	}
    }

    /**
     * Perform an interactive login on AIX by reading the prompt from the input stream, and entering CR after the password.
     * Then read one line.
     */
    private void loginAIX() throws Exception {
	setInteractive(true);
	p.start();
	getInputStream();
	byte[] buff = new byte[17]; //root's Password:_
	in.readFully(buff);
	getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write(CR);
	out.flush();
	in.readLine();
    }

    /**
     * Perform an interactive login on Solaris by reading the prompt from the input stream, entering CR, then skipping past
     * the Message Of The Day (if any).
     */
    private void loginSolaris() throws Exception {
	setInteractive(true);
	p.start();
	getInputStream();
	byte[] buff = new byte[10]; //Password:_
	in.readFully(buff);
	getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write(CR);
	out.flush();
	String line1 = in.readLine();
	if (line1 == null) {
	    throw new EOFException(null);
	} else if (line1.indexOf("Sorry") == -1) {
	    //
	    // Skip past the message of the day
	    // NOTE -- by omitting the "-" (login) argument from the su command, we need no longer skip the MOTD.
	    //
/*
	    int linesToSkip = us.getMotdLines();
	    for (int i=0; i < linesToSkip; i++) {
		if (in.readLine() == null) {
		    throw new EOFException(null);
		}
	    }
*/
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_AUTHENTICATION_FAILED, cred.getUsername());
	    throw new LoginException(msg);
	}
    }

    private String getSuString(String command) {
	StringBuffer sb = new StringBuffer();
	switch(us.getFlavor()) {
	  case MACOSX:
	    sb.append("sudo -E -p ? ").append(command);
	    break;

	  case LINUX:
	    sb.append("su ");
	    sb.append(cred.getUsername());
	    sb.append(" -m -c \"");
	    sb.append(command.replace("\"", "\\\""));
	    sb.append("\"");
	    break;

	  default:
	    sb.append("su ");
	    sb.append(cred.getUsername());
	    sb.append(" -c \"");
	    sb.append(command.replace("\"", "\\\""));
	    sb.append("\"");
	    break;
	}
	return sb.toString();
    }
}
