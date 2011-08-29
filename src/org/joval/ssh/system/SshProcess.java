// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.system.IProcess;
import org.joval.io.StreamLogger;
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
    private boolean debug=false, interactive = false;
    private StreamLogger debugIn, debugErr;
    private long timeout;

    private static int num = 0;

    SshProcess(ChannelExec ce, String command) {
	this(ce, command, 3600000L, false);
    }

    SshProcess(ChannelExec ce, String command, long millis, boolean debug) {
	this.ce = ce;
	this.command = command;
	timeout = millis;
	this.debug = debug;
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
	if (debug) {
	    debugIn = new StreamLogger(ce.getInputStream(), new File("out." + num + ".log"));
	    debugIn.start();
	    debugErr = new StreamLogger(ce.getErrStream(), new File("err." + num + ".log"));
	    debugErr.start();
	    num++;
	}
    }

    public InputStream getInputStream() {
	if (debug) {
	    return debugIn;
	}
	try {
	    return ce.getInputStream();
	} catch (IOException e) {
	}
	return null;
    }

    public InputStream getErrorStream() {
	if (debug) {
	    return debugErr;
	}
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
	if (debug) {
	    try {
		debugIn.close();
	    } catch (IOException e) {
	    }
	    try {
		debugErr.close();
	    } catch (IOException e) {
	    }
	}
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
		p.waitFor(timeout); // 1 hour
	    } catch (InterruptedException e) {
	    } finally {
		if (!ce.isEOF()) {
		    p.destroy();
		}
	    }
	}
    }
}
