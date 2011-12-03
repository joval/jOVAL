// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.joval.intf.identity.ICredential;
import org.joval.intf.io.IReader;
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
    private UnixSession us;
    private IProcess p;
    private ICredential cred;
    private String innerCommand;
    private long timeout;
    private ReaderInputStream in=null, err=null;
    private OutputStream out=null;

    private static long readTimeout = JOVALSystem.getLongProperty(JOVALSystem.PROP_SUDO_READ_TIMEOUT);
    private static int execRetries = JOVALSystem.getIntProperty(JOVALSystem.PROP_SUDO_MAX_RETRIES);

    Sudo(UnixSession us, ICredential cred, String cmd, long ms) throws Exception {
	this.us = us;
	ssh = us.ssh;
	this.cred = cred;
	innerCommand = cmd;
	timeout = ms;
	p = ssh.createProcess(getSuString(cmd), ms);
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
	  case SOLARIS: {
	    boolean success = false;
	    for (int attempt=0; !success; attempt++) {
		try {
		    setInteractive(true);
		    p.start();
		    getInputStream();

		    //
		    // Read the Password prompt
		    //
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
			success = true;
		    } else {
			String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_AUTHENTICATION_FAILED, cred.getUsername());
			throw new LoginException(msg);
		    }

		//
		// While all this is going on, the underlying SshProcess may time out and close the streams.
		//
		} catch (EOFException e) {
		    if (attempt > execRetries) {
			JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PROCESS_RETRY, innerCommand, attempt);
			throw e;
		    } else {
			JOVALSystem.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			// try again
			in = null;
			out = null;
			p = ssh.createProcess(getSuString(innerCommand), timeout);
			JOVALSystem.getLogger().debug(JOVALMsg.STATUS_PROCESS_RETRY, innerCommand);
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
	    in = new ReaderInputStream(StreamTool.getSafeReader(p.getInputStream(), readTimeout));
	}
	return in;
    }

    public InputStream getErrorStream() {
	if (err == null) {
	    err = new ReaderInputStream(StreamTool.getSafeReader(p.getErrorStream(), readTimeout));
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

    private class ReaderInputStream extends InputStream {
	IReader reader;

	ReaderInputStream(IReader reader) {
	    this.reader = reader;
	}

	public int read() throws IOException {
	    return reader.read();
	}

	public void close() throws IOException {
	    reader.close();
	}

	public String readLine() throws IOException {
	    return reader.readLine();
	}

	public void readFully(byte[] buff) throws IOException {
	    reader.readFully(buff);
	}
    }
}
