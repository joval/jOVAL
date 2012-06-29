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

import org.vngx.jsch.Channel;
import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelShell;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.PerishableReader;
import org.joval.io.StreamLogger;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;

/**
 * An abstract implementation of an IProcess based on an SSH channel.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class SshProcess implements IProcess {
    public enum Type {
	EXEC, SHELL, POSIX;
    }

    Channel channel;
    String command;
    boolean debug;
    InputStream in = null, err = null;
    OutputStream out = null;
    boolean interactive=false, dirty = true, running = false;
    File wsdir;
    LocLogger logger;
    int pid;
    String[] env;

    SshProcess(String command, String[] env, boolean debug, File wsdir, int pid, LocLogger logger) {
	this.command = command;
	this.env = env;
	this.debug = debug;
	this.wsdir = wsdir;
	this.pid = pid;
	this.logger = logger;
    }

    // Implement IProcess

    public String getCommand() {
	return command;
    }

    public void setInteractive(boolean interactive) {
	this.interactive = interactive;
    }

    public InputStream getInputStream() throws IOException {
	if (in == null) {
	    if (debug) {
		File f = null;
		if (wsdir == null) {
		    f = new File("out." + pid + ".log");
		} else {
		    f = new File(wsdir, "out." + pid + ".log");
		}
		in = new StreamLogger(command, channel.getInputStream(), f);
	    } else {
		in = channel.getInputStream();
	    }
	}
	return in;
    }

    public InputStream getErrorStream() throws IOException {
	if (err == null) {
	    if (debug) {
		File f = null;
		if (wsdir == null) {
		    f = new File("err." + pid + ".log");
		} else {
		    f = new File(wsdir, "err." + pid + ".log");
		}
		err = new StreamLogger(command, channel.getExtInputStream(), f);
	    } else {
		err = channel.getExtInputStream();
	    }
	}
	return err;
    }

    public OutputStream getOutputStream() throws IOException {
	if (out == null) {
	    out = channel.getOutputStream();
	}
	return out;
    }

    public void waitFor(long millis) throws InterruptedException {
	long end = Long.MAX_VALUE;
	if (millis > 0) {
	    end = System.currentTimeMillis() + millis;
	}
	while (isRunning() && System.currentTimeMillis() < end) {
	    Thread.sleep(Math.min(end - System.currentTimeMillis(), 250));
	}
    }

    public synchronized void destroy() {
	logger.warn(JOVALMsg.ERROR_PROCESS_KILL, command);
	try {
	    if (channel.isConnected()) {
		channel.sendSignal("KILL");
		channel.getInputStream().close();
		channel.getExtInputStream().close();
	    }
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    cleanup();
	}
    }

    public boolean isRunning() {
	if (!running) {
	    return false;
	} else {
	    if (channel != null && channel.isConnected()) {
		if (channel.isEOF()) {
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

    public int exitValue() throws IllegalThreadStateException {
	if (isRunning()) {
	    throw new IllegalThreadStateException(command);
	}
	return channel.getExitStatus();
    }

    // Private

    /**
     * This method is always called by the first thread to notice that the process is finished (or the first thread to
     * destroy the process).
     */
    private synchronized void cleanup() {
	if (!dirty) {
	    return;
	}
	if (in != null) {
	    try {
		in.close();
	    } catch (IOException e) {
	    }
	}
	if (err != null) {
	    try {
		err.close();
	    } catch (IOException e) {
	    }
	}
	if (out != null) {
	    try {
		out.close();
	    } catch (IOException e) {
	    }
	}
	if (channel.isConnected()) {
	    if (!channel.isEOF()) {
		logger.debug(JOVALMsg.ERROR_SSH_PROCESS_DESTROY, command);
	    }
	    channel.disconnect();
	}
	logger.debug(JOVALMsg.STATUS_SSH_PROCESS_END, command);
	dirty = false;
	running = false;
    }
}
