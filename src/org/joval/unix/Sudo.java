// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.joval.intf.identity.ICredential;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamTool;
import org.joval.util.JOVALSystem;

/**
 * A tool for running processes as a specific user.  This does not use the sudo command, rather, it makes use of the
 * su command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Sudo implements IProcess {
    private IProcess p;
    private UnixFlavor flavor;
    private ICredential cred;
    private String innerCommand;
    private InputStream in=null, err=null;
    private OutputStream out=null;

    public Sudo(IProcess p, UnixFlavor flavor, ICredential cred) {
	this.p = p;
	this.flavor = flavor;
	this.cred = cred;
	setCommand(p.getCommand());
    }

    // Implement IProcess

    public String getCommand() {
	return innerCommand;
    }

    public void setCommand(String command) {
	innerCommand = command;
	p.setCommand(getSuString(command));
    }

    public void setInteractive(boolean interactive) {
	p.setInteractive(interactive);
    }

    public void start() throws Exception {
	if (cred.getPassword() == null) {
	    throw new CredentialException(JOVALSystem.getMessage("ERROR_MISSING_PASSWORD", cred.getUsername()));
	}
	switch(flavor) {
	  case SOLARIS: {
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
		throw new LoginException(JOVALSystem.getMessage("ERROR_AUTHENTICATION_FAILED", cred.getUsername()));
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
