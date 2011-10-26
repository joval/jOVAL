// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.joval.intf.identity.ICredential;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamTool;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A tool for running processes as a specific user.  This does not use the sudo command, rather, it makes use of the
 * su command.  It is used exclusively by the remote Unix session classes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Sudo implements IProcess {
    private SshSession ssh;
    private IProcess p;
    private IUnixSession.Flavor flavor;
    private ICredential cred;
    private String innerCommand;
    private long timeout;
    private boolean debug;
    private InputStream in=null, err=null;
    private OutputStream out=null;

    Sudo(SshSession ssh, IUnixSession.Flavor flavor, ICredential cred, String cmd, long ms, boolean debug) throws Exception {
	this.ssh = ssh;
	this.cred = cred;
	this.flavor = flavor;
	innerCommand = cmd;
	timeout = ms;
	this.debug = debug;
	p = ssh.createProcess(getSuString(cmd), ms, debug);
    }

    // Implement IProcess

    public void setInteractive(boolean interactive) {
	p.setInteractive(interactive);
    }

    public void start() throws Exception {
	if (cred.getPassword() == null) {
	    throw new CredentialException(JOVALSystem.getMessage(JOVALMsg.ERROR_MISSING_PASSWORD, cred.getUsername()));
	}
	switch(flavor) {
	  case SOLARIS: {
	    boolean success = false;
	    for (int i=0; !success; i++) {
		try {
		    setInteractive(true);
		    p.start();
		    getInputStream();

		    byte[] buff = new byte[10]; //Password:_
		    StreamTool.readFully(in, buff);

		    getOutputStream();
		    out.write(cred.getPassword().getBytes());
		    out.write('\n');
		    out.flush();
		    getInputStream();
		    String line1 = readLine();
		    if (line1.indexOf("Sorry") == -1) {
			String line2 = readLine();
		    } else {
			String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_AUTHENTICATION_FAILED, cred.getUsername());
			throw new LoginException(msg);
		    }
		    success = true;
		} catch (EOFException e) {
		    if (i > 2) {
			JOVALSystem.getLogger().warn(JOVALMsg.ERROR_SSH_PROCESS_RETRY, innerCommand, i);
			throw e;
		    } else {
			JOVALSystem.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			// try again
			in = null;
			out = null;
			p = ssh.createProcess(getSuString(innerCommand), timeout, debug);
			JOVALSystem.getLogger().warn(JOVALMsg.STATUS_SSH_PROCESS_RETRY, innerCommand);
		    }
		}
	    }
	    break;
	  }

	  default:
	  case LINUX: {
	    p.start();
	    getOutputStream();
	    out.write(cred.getPassword().getBytes());
	    out.write('\n');
	    out.flush();
	    break;
	  }
	}
    }

    public InputStream getInputStream() {
	if (in == null) {
	    in = p.getInputStream();
	}
	return in;
    }

    public InputStream getErrorStream() {
	if (err == null) {
	    err = p.getErrorStream();
	}
	return err;
    }

    public OutputStream getOutputStream() {
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

    // Private

    private String getSuString(String command) {
	StringBuffer sb = new StringBuffer("su - ");
	sb.append(cred.getUsername());
	sb.append(" -c \"");
	sb.append(command.replace("\"", "\\\""));
	sb.append("\"");
	return sb.toString();
    }

    private String readLine() throws IOException {
	int ch=0, len=0;
	byte[] buff = new byte[512];
	while((ch = in.read()) != -1 && ch != 10 && len < buff.length) { // 10 == \n
	    buff[len++] = (byte)ch;
	}
	return new String(buff, 0, len);
    }
}
