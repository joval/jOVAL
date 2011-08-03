// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix.remote.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.exception.JSchException;

import org.joval.unix.remote.UnixCredential;
import org.joval.intf.system.IProcess;
import org.joval.io.StreamTool;
import org.joval.util.JOVALSystem;

/**
 * A representation of a process.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SshProcess implements IProcess {
    private ChannelExec ce;
    private String rootPassword;
    private InputStream in, err;
    private OutputStream out;

    /**
     * If the UnixCredential contains a root password, then the command is automatically run as root using su.
     */
    SshProcess(ChannelExec ce, String command, UnixCredential cred) {
	this.ce = ce;
	rootPassword = cred.getRootPassword();
	if (rootPassword != null) {
	    command = "echo " + cred.getRootPassword() + " | su - root -c \"" + quoteEscape(command) + "\"";
	}
	ce.setCommand(command);
    }

    // Implement IProcess

    public void start() throws Exception {
	ce.connect();
	new Monitor(this).start();
	if (rootPassword != null) {
	    getOutputStream();
	    out.write(rootPassword.getBytes());
	    out.write('\n');
	    out.flush();
	}
    }

    public InputStream getInputStream() {
	try {
	    if (in == null) {
	        in = ce.getInputStream();
	    }
	    return in;
	} catch (IOException e) {
	}
	return null;
    }

    public InputStream getErrorStream() {
	try {
	    if (err == null) {
		err = ce.getErrStream();
	    }
	    return err;
	} catch (IOException e) {
	}
	return null;
    }

    public OutputStream getOutputStream() {
	try {
	    if (out == null) {
		out = ce.getOutputStream();
	    }
	    return out;
	} catch (IOException e) {
	}
	return null;
    }

    public void waitFor(long millis) throws InterruptedException {
	long end = Long.MAX_VALUE;
	if (millis > 0) {
	    end = System.currentTimeMillis() + millis;
	}
	while (!ce.isEOF() && System.currentTimeMillis() < end) {
	    Thread.sleep(Math.min(end - System.currentTimeMillis(), 250));
	}
    }

    public int exitValue() throws IllegalThreadStateException {
	return ce.getExitStatus();
    }

    public void destroy() {
	try {
	    ce.sendSignal("KILL");
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	} finally {
	    cleanup();
	}
    }

    // Private

    private String quoteEscape(String s) {
	return s.replace("\"", "\\\"");
    }

    private synchronized void cleanup() {
	if (ce.isConnected()) {
	    ce.disconnect();
	}
    }

    // Private

    class Monitor implements Runnable {
	SshProcess p;

	Monitor(SshProcess p) {
	    this.p = p;
	}

	void start() {
	    new Thread(this).start();
	}

	public void run() {
	    try {
		p.waitFor(3600000); // 1 hour
	    } catch (InterruptedException e) {
	    } finally {
		p.cleanup();
	    }
	}
    }
}
