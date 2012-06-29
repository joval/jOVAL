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
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.PerishableReader;
import org.joval.io.StreamLogger;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;

/**
 * A representation of a process.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SshProcess implements IProcess {
    private Channel channel;
    private String command;
    private boolean debug, shell;
    private InputStream in = null, err = null;
    private OutputStream out = null;
    private boolean interactive=false, dirty = true, running = false;
    private File wsdir;
    private LocLogger logger;
    private int pid;
    private String[] env;
    private int exitValue = -1;

    SshProcess(Channel channel, String command, String[] env, boolean debug, File wsdir, int pid, LocLogger logger) {
	this.channel = channel;
	shell = channel.getType() == ChannelType.SHELL;
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

    static final int CR = '\r';

    public void start() throws Exception {
	logger.debug(JOVALMsg.STATUS_PROCESS_START, command);
	switch(channel.getType()) {
	  case SHELL: {
	    ChannelShell shell = (ChannelShell)channel;
	    shell.setPty(true);
	    byte[] mode = {0x35, 0x00, 0x00, 0x00, 0x00, 0x00}; // echo off
	    shell.setTerminalMode(mode);
	    shell.connect();
	    getOutputStream();
	    PerishableReader reader = PerishableReader.newInstance(getInputStream(), 10000L);
	    determinePrompt(reader); // garbage - may include MOTD
	    out.write("/bin/sh\r".getBytes());
	    out.flush();
	    String prompt = determinePrompt(reader);
	    if (env != null) {
		for (String var : env) {
		    int ptr = var.indexOf("=");
		    if (ptr > 0) {
			StringBuffer setenv = new StringBuffer(var).append("; export ").append(var.substring(0,ptr));
			out.write(setenv.toString().getBytes());
			out.write(CR);
			out.flush();
			reader.readUntil(prompt); // ignore
		    }
		}
	    }
	    in = new MarkerTerminatedInputStream(reader, prompt);
	    out.write(command.getBytes());
	    out.write(CR);
	    out.flush();
	    running = true;
	    break;
	  }

	  case EXEC: {
	    ChannelExec ce = (ChannelExec)channel;
	    ce.setPty(interactive);
	    ce.setCommand(command);
	    channel.connect();
	    running = true;
	    break;
	  }

	  default:
	    throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_SSH_CHANNEL, channel.getType()));
	}
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

    public int exitValue() throws IllegalThreadStateException {
	if (isRunning()) {
	    throw new IllegalThreadStateException(command);
	}
	if (shell) {
	    return exitValue;
	} else {
	    return channel.getExitStatus();
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
		logger.debug(JOVALMsg.ERROR_PROCESS_DESTROY, command);
	    }
	    channel.disconnect();
	}
	logger.debug(JOVALMsg.STATUS_PROCESS_END, command);
	dirty = false;
	running = false;
    }

    private String determinePrompt(InputStream in) throws IOException {
	int ch = -1;
	boolean trigger = false;
	StringBuffer sb = new StringBuffer();
	while ((ch = in.read()) != -1) {
	    switch(ch) {
	      case '>':
	      case '$':
	      case '#':
		sb.append((char)ch);
		trigger = true;
		break;

	      case ' ':
		sb.append((char)ch);
		if (trigger) {
		    return sb.toString();
		}
		break;

	      default:
		sb.append((char)ch);
		trigger = false;
		break;
	    }
	}
	return null;
    }

    class MarkerTerminatedInputStream extends PerishableReader {
	String marker;
	byte[] markerBytes;

	MarkerTerminatedInputStream(InputStream in, String marker) {
	    super(in, 10000L);
	    this.marker = marker;
	    markerBytes = marker.getBytes();
	}

	@Override
	public int read(byte[] buff, int offset, int len) throws IOException {
	    int bytesRead = 0;
	    if (buffer.hasNext()) {
		for (int i=offset; i < len && buffer.hasNext(); i++) {
		    buff[i] = (byte)(buffer.next() & 0xFF);
		    bytesRead++;
		}
		reset();
		return bytesRead;
	    } else if (!buffer.isEmpty()) {
		buffer.clear();
	    }

	    int avail = available();
	    if (avail == 0) {
		for (int i=offset; i < len; i++) {
		    int ch = read();
		    if (ch == -1) {
			break;
		    } else {
			buff[i] = (byte)ch;
			bytesRead++;
		    }
		}
		return bytesRead;
	    } else {
		byte[] temp = new byte[Math.min(avail, 512)];
		int tempLen = in.read(temp);
		for (int i=0; i < tempLen; i++) {
		    if (temp[i] == markerBytes[0] && markerTest(temp, i) == -1) {
			System.arraycopy(temp, 0, buff, offset, i);
			return i;
		    }
		}
		System.arraycopy(temp, 0, buff, offset, tempLen);
		return tempLen;
	    }
	}

	@Override
	public int read() throws IOException {
	    if (isEOF) {
		return -1;
	    }
	    if (buffer.hasNext()) {
		reset();
		return buffer.next();
	    } else if (!buffer.isEmpty()) {
		buffer.clear();
	    }
	    int ch = in.read();
	    reset();
	    if (ch == -1) {
		isEOF = true;
	    } else {
		if (buffer.hasCapacity()) {
		    buffer.add((byte)(ch & 0xFF));
		} else {
		    buffer.clear(); // buffer overflow
		}
		if (ch == (int)markerBytes[0]) {
		    return markerTest();
		}
	    }
	    return ch;
	}

	private int markerTest() throws IOException {
	    return markerTest(null, 0);
	}

	private int markerTest(byte[] buff, int offset) throws IOException {
	    boolean overflow = false;
	    for (int i=1; i < markerBytes.length; i++) {
		byte b;
		if (buff != null && (offset + i) < buff.length) {
		    b = buff[offset + i];
		} else {
		    if (!overflow) {
			setCheckpoint(markerBytes.length - i);
			overflow = true;
		    }
		    b = (byte)(in.read() & 0xFF);
		    buffer.add(b);
		    reset();
		}
		if (b != markerBytes[i]) {
		    if (overflow) {
			restoreCheckpoint();
		    }
		    return markerBytes[0];
		}
	    }
	    try {
		out.write("echo $?".getBytes());
		out.write(CR);
		out.flush();
		PerishableReader pr = PerishableReader.newInstance(in, 0);
		exitValue = Integer.parseInt(pr.readUntil(marker).trim());
		pr.close();
		close();
		running = false;
		channel.disconnect();
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    isEOF = true;
	    return -1;
	}
    }
}
