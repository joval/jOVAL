// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamLogger;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;
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
    private boolean dirty = true, running = false;
    private LocLogger logger;

    private static int num = 0;

    SshProcess(ChannelExec ce, String command, boolean debug, LocLogger logger) {
	this.ce = ce;
	this.command = command;
	this.debug = debug;
	this.logger = logger;
    }

    // Implement IProcess

    public String getCommand() {
	return command;
    }

    public void setInteractive(boolean interactive) {
	this.interactive = interactive;
    }

    public void start() throws Exception {
	logger.debug(JOVALMsg.STATUS_PROCESS_START, command);
	ce.setPty(interactive);
	ce.setCommand(command);
	ce.connect();
	if (debug) {
	    num++;
	}
	running = true;
    }

    public InputStream getInputStream() throws IOException {
	if (debug) {
	    if (debugIn == null) {
		debugIn = new StreamLogger(command, ce.getInputStream(), new File("out." + num + ".log"), logger);
	    }
	    return debugIn;
	} else {
	    return ce.getInputStream();
	}
    }

    public InputStream getErrorStream() throws IOException {
	if (debug) {
	    if (debugErr == null) {
		debugErr = new StreamLogger(command, ce.getErrStream(), new File("err." + num + ".log"), logger);
	    }
	    return debugErr;
	} else {
	    return ce.getErrStream();
	}
    }

    public OutputStream getOutputStream() throws IOException {
	return ce.getOutputStream();
    }

    public void waitFor(long millis) throws InterruptedException {
	long end = Long.MAX_VALUE;
	if (millis > 0) {
	    end = System.currentTimeMillis() + millis;
	}
	while (!ce.isEOF() && System.currentTimeMillis() < end) {
	    Thread.sleep(Math.min(end - System.currentTimeMillis(), 250));
	}
	if (ce.isEOF()) {
	    cleanup();
	}
    }

    public int exitValue() throws IllegalThreadStateException {
	return ce.getExitStatus();
    }

    public synchronized void destroy() {
	logger.warn(JOVALMsg.ERROR_PROCESS_KILL, command);
	try {
	    if (ce.isConnected()) {
		ce.sendSignal("KILL");
	    }
	} catch (Exception e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    cleanup();
	}
    }

    public boolean isRunning() {
	if (!running) {
	    return false;
	} else {
	    if (ce != null && ce.isConnected()) {
		if (ce.isEOF()) {
		    cleanup();
		    return false;
		} else {
		    return true;
		}
	    } else {
		running = false;
		return false;
	    }
	}
    }

    // Private

    private String quoteEscape(String s) {
	return s.replace("\"", "\\\"");
    }

    /**
     * This method is always called by the first thread to notice that the process is finished (or the first thread to
     * destroy the process).
     */
    private synchronized void cleanup() {
	if (!dirty) {
	    return;
	}
	if (debug) {
	    if (debugIn != null) {
		try {
		    debugIn.close();
		} catch (IOException e) {
		}
	    }
	    if (debugErr != null) {
		try {
		    debugErr.close();
		} catch (IOException e) {
		}
	    }
	}
	if (ce.isConnected()) {
	    if (!ce.isEOF()) {
		logger.debug(JOVALMsg.ERROR_PROCESS_DESTROY, command);
	    }
	    ce.disconnect();
	}
	logger.trace(JOVALMsg.STATUS_PROCESS_END, command);
	dirty = false;
	running = false;
    }
}
