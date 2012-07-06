// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.File;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelShell;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.io.PerishableReader;
import org.joval.intf.io.IReader;
import org.joval.intf.ssh.system.IShell;
import org.joval.intf.ssh.system.IShellProcess;
import org.joval.intf.system.IProcess;
import org.joval.util.JOVALMsg;
import org.joval.util.SessionException;
import org.joval.util.StringTools;

/**
 * A basic SSH shell-channel-based IProcess implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class BasicShellProcess extends SshProcess implements IShell, IShellProcess {
    static final int CR = '\n';

    /**
     * RFC4254 terminal mode byte stream
     */
    static final byte[] MODE = {0x35, 0x00,0x00,
				0x00, 0x00,0x00};

    enum Mode {
	INACTIVE, SHELL, PROCESS;
    }

    private boolean keepAlive = false;

    Type type = Type.SHELL;
    int exitValue = -1;
    String prompt;
    Mode mode;

    /**
     * A basic shell-based IProcess that also implements the recyclable interface.
     *
     * @param command Set to null to initialize as a shell, or non-null to initialize as a process.
     */
    BasicShellProcess(Session session, String command, boolean debug, File wsdir, int pid, LocLogger logger)
		throws JSchException {

	super(command, debug, wsdir, pid, logger);
	if (command == null) {
	    mode = Mode.INACTIVE;
	} else {
	    mode = Mode.PROCESS;
	}
	channel = session.openChannel(ChannelType.SHELL);
    }

    // Implement IShell

    public boolean ready() {
	return !running;
    }

    public synchronized void println(String str) throws IllegalStateException, SessionException, IOException {
	if (running) {
	    throw new IllegalStateException(command);
	}
	if (!channel.isConnected()) {
	    connect();
	}
	mode = Mode.SHELL;
	logger.debug(JOVALMsg.STATUS_SSH_SHELL_PRINTLN, str);
	out.write(str.getBytes(StringTools.ASCII));
	out.write(CR);
	out.flush();
    }

    public String read(long timeout) throws IllegalStateException, IOException {
	switch(mode) {
	  case SHELL:
	    return readInternal(timeout);

	  default:
	    throw new IllegalStateException(mode.toString());
	}
    }

    public String readLine(long timeout) throws IllegalStateException, IOException {
	switch(mode) {
	  case SHELL:
	    return readLineInternal(timeout);

	  default:
	    throw new IllegalStateException(mode.toString());
	}
    }

    public String getPrompt() {
	return prompt;
    }

    public void close() throws IllegalStateException {
	switch(mode) {
	  case PROCESS:
	    throw new IllegalStateException(mode.toString());

	  case SHELL:
	    try {
		// an active shell; drain any unread data
		read(1000L);
		mode = Mode.INACTIVE;
	    } catch (IOException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;
	}
	logger.debug(JOVALMsg.STATUS_SSH_SHELL_DETACH);
	if (!keepAlive) {
	    channel.disconnect();
	}
    }

    // Implement IShellProcess

    public void setKeepAlive(boolean keepAlive) {
	this.keepAlive = keepAlive;
    }

    public boolean isAlive() {
	return channel.isConnected();
    }

    public IProcess newProcess(String command) throws IllegalStateException {
	switch(mode) {
	  case SHELL:
	    throw new IllegalStateException(mode.toString());

	  case PROCESS:
	    throw new IllegalStateException(this.command);

	  case INACTIVE:
	  default:
	    mode = Mode.PROCESS;
	    this.command = command;
	    return this;
	}
    }

    public IShell getShell() throws IllegalStateException {
	if (mode == Mode.PROCESS) {
	    throw new IllegalStateException(mode.toString());
	}
	logger.debug(JOVALMsg.STATUS_SSH_SHELL_ATTACH);
	return this;
    }

    // Implement IProcess

    @Override
    public synchronized void start() throws Exception {
	switch(mode) {
	  case SHELL:
	  case INACTIVE:
	    throw new IllegalStateException(mode.toString());
	}

	logger.debug(JOVALMsg.STATUS_SSH_PROCESS_START, type, command);
	if (!channel.isConnected()) {
	    connect();
	}
	in = new MarkerTerminatedInputStream(in, prompt.getBytes(StringTools.ASCII));
	out.write(command.getBytes(StringTools.ASCII));
	out.write(CR);
	out.flush();
	running = true;
    }

    @Override
    public int exitValue() throws IllegalThreadStateException {
	switch(mode) {
	  case SHELL:
	  case PROCESS:
	    throw new IllegalThreadStateException(mode.toString());
	}
	return exitValue;
    }

    @Override
    public boolean isRunning() {
	return running;
    }

    // Internal

    /**
     * Get the exit code of the last command, given a reader to the shell input.
     */
    int getExitValueInternal(IReader reader) throws Exception {
	return -1;
    }

    /**
     * Connect the shell channel, determine the prompt, and set up streams.
     */
    final void connect() throws IOException, SessionException {
	((ChannelShell)channel).setPty(true);
	((ChannelShell)channel).setTerminalMode(MODE);
	try {
	    channel.connect();
	} catch (JSchException e) {
	    throw new SessionException(e);
	}
	in = new KeepAliveInputStream(getInputStream());
	//
	// Determine the prompt by temporarily assuming SHELL mode and using the internal read method.
	//
	readInternal(10000L);
	out = new KeepAliveOutputStream(getOutputStream());
	err = new KeepAliveInputStream(getErrorStream());
    }

    synchronized String readInternal(long timeout) throws IOException {
	StringBuffer sb = null;
	String line = null;
	while((line = readLineInternal(timeout)) != null) {
	    if (sb == null) {
		sb = new StringBuffer();
	    } else {
		sb.append("\n");
	    }
	    sb.append(line);
	}
	if (sb == null) {
	    return null;
	} else {
	    return sb.toString();
	}
    }

    synchronized String readLineInternal(long timeout) throws IOException {
	PerishableReader reader = PerishableReader.newInstance(in, timeout);
	try {
	    StringBuffer sb = new StringBuffer();
	    int ch = -1;
	    while((ch = reader.read()) != -1) {
		switch(ch) {
		  case '\r':
		    if (in.markSupported() && in.available() > 0) {
			in.mark(1);
			switch(in.read()) {
			  case '\n':
			    return sb.toString();
			  default:
			    in.reset();
			    break;
			}
		    }
		    // fall-thru
		  case '\n':
		    return sb.toString();
		  default:
		    sb.append((char)(ch & 0xFF));
		}
		if (in.available() == 0 && isPrompt(sb.toString())) {
		    prompt = sb.toString();
		    mode = Mode.INACTIVE;
		    return null;
		}
	    }
	} finally {
	    reader.defuse();
	}
	return null;
    }

    class KeepAliveInputStream extends InputStream {
	private InputStream in;

	KeepAliveInputStream(InputStream in) {
	    this.in = in;
	}

	public int available() throws IOException {
	    return in.available();
	}

	public int read() throws IOException {
	    return in.read();
	}

	public int read(byte[] buff) throws IOException {
	    return in.read(buff, 0, buff.length);
	}

	public int read(byte[] buff, int offset, int len) throws IOException {
	    return in.read(buff, offset, len);
	}

	public void close() throws IOException {
	    if (!keepAlive) {
		in.close();
	    }
	}

	public boolean markSupported() {
	    return in.markSupported();
	}

	public void mark(int readlimit) {
	    in.mark(readlimit);
	}

	public void reset() throws IOException {
	    in.reset();
	}
    }

    class KeepAliveOutputStream extends OutputStream {
	private OutputStream out;

	KeepAliveOutputStream(OutputStream out) {
	    this.out = out;
	}

	public void write(int i) throws IOException {
	    out.write(i);
	}

	public void write(byte[] buff, int offset, int len) throws IOException {
	    out.write(buff, offset, len);
	}

	public void flush() throws IOException {
	    out.flush();
	}

	public void close() throws IOException {
	    if (!keepAlive) {
		out.close();
	    }
	}
    }

    /**
     * An input that reads until a character sequence is reached, then behaves as if it's ended.
     */
    class MarkerTerminatedInputStream extends PerishableReader {
	private boolean forInternalExitCode = false;
	byte[] markerBytes;

	/**
	 * Create a new input stream with the given marker.
	 */
	MarkerTerminatedInputStream(InputStream in, byte[] markerBytes) {
	    super(in, 10000L);
	    this.markerBytes = markerBytes;
	}

	byte[] getMarker() {
	    return markerBytes;
	}

	/**
	 * Buffered read, for better performance.
	 */
	@Override
	public int read(byte[] buff, int offset, int len) throws IOException {
	    int bytesRead = 0;
	    if (buffer.hasNext()) {
		for (int i=0; i < len && buffer.hasNext(); i++) {
		    buff[offset + i] = (byte)(buffer.next() & 0xFF);
		    bytesRead++;
		}
		reset();
		return bytesRead;
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
		byte[] temp = new byte[Math.min(avail, buff.length)];
		int tempLen = in.read(temp);
		for (int i=0; i < tempLen; i++) {
		    if (temp[i] == markerBytes[0] && markerTest(temp, i) == -1) {
			System.arraycopy(temp, 0, buff, offset, i);
			buffer.add(temp, 0, i);
			return i;
		    }
		}
		System.arraycopy(temp, 0, buff, offset, tempLen);
		buffer.add(temp, 0, tempLen);
		return tempLen;
	    }
	}

	@Override
	public void close() throws IOException {
	    super.close();
	    if (!keepAlive) {
		channel.disconnect();
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

	/**
	 * This method is called whenever the first byte of the marker is read from input. If
	 * the marker is read fully, then -1 is returned.  Otherwise, the stream position is
	 * reset to its original position, and the first byte of the marker is returned.
	 */
	private int markerTest() throws IOException {
	    return markerTest(null, 0);
	}

	/**
	 * A variant of the markerTest, that begins comparing from a buffer before reading from
	 * the input stream.
	 */
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
	    if (!forInternalExitCode) {
		try {
		    MarkerTerminatedInputStream childInput = new MarkerTerminatedInputStream(in, markerBytes);
		    childInput.forInternalExitCode = true;
		    exitValue = getExitValueInternal(childInput);
		    childInput.close();
		    close();
		    running = false;
		    mode = Mode.INACTIVE;
		    BasicShellProcess.this.in = in;
		    logger.debug(JOVALMsg.STATUS_SSH_PROCESS_END, command);
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	    isEOF = true;
	    return -1;
	}
    }

    // Private

    private boolean isPrompt(String s) {
	return (s.endsWith("> ") || s.endsWith(">") ||
		s.endsWith("# ") || s.endsWith("#") ||
		s.endsWith("$ ") || s.endsWith("$") ||
		s.endsWith("? ") || s.endsWith("?"));
    }
}
