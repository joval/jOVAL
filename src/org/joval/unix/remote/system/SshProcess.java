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
    private String command;
    private boolean interactive = false;

    SshProcess(ChannelExec ce, String command) {
	this.ce = ce;
	this.command = command;
    }

    // Implement IProcess

    public String getCommand() {
	return command;
    }

    public void setCommand(String command) {
	this.command = command;
    }

    public void setInteractive(boolean interactive) {
	this.interactive = interactive;
    }

    public void start() throws Exception {
	ce.setPty(interactive);
	ce.setCommand(command);
	ce.connect();
	new Monitor(this).start();
    }

    public InputStream getInputStream() {
	try {
	    return ce.getInputStream();
	} catch (IOException e) {
	}
	return null;
    }

    public InputStream getErrorStream() {
	try {
	    return ce.getErrStream();
	} catch (IOException e) {
	}
	return null;
    }

    public OutputStream getOutputStream() {
	try {
	    return ce.getOutputStream();
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
