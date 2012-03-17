// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ConcurrentModificationException;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IReader;
import org.joval.intf.util.IPerishable;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A PerishableReader is a class that implements both IReader and IPerishable, signifying input that has a potential to
 * expire.  Instances are periodically checked to see if they've been blocking on a read operation beyond the set expiration
 * timeout.  In that event, the underlying stream is closed so that the blocking Thread can continue.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PerishableReader extends InputStream implements IReader, IPerishable {
    /**
     * Create a new instance using the given InputStream and initial timeout.  The clock begins ticking immediately, so
     * it is important to start reading before the timeout has expired.
     *
     * If the specified InputStream is already a PerishableReader, then its timeout is altered and it is returned.
     *
     * @arg maxTime the maximum amount of time that should be allowed to elapse between successful reads, in milliseconds.
     *              If maxTime <= 0, the default of 1hr will apply.
     */
    public static PerishableReader newInstance(InputStream in, long maxTime) {
	PerishableReader reader = null;
	if (in instanceof PerishableReader) {
	    reader = (PerishableReader)in;
	    reader.setTimeout(maxTime);
	} else {
	    reader = new PerishableReader(in, maxTime);
	}
	return reader;
    }

    protected InputStream in;
    protected boolean isEOF;
    protected Buffer buffer;
    private boolean closed, expired;
    private long timeout;
    private TimerTask task;
    private LocLogger logger;
    private StackTraceElement[] trace;

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IReader

    public synchronized void close() throws IOException {
	if (!closed)  {
	    defuse();
	    in.close();
	    closed = true;
	}
    }

    public boolean checkClosed() {
	return closed;
    }

    public boolean checkEOF() {
	return isEOF;
    }

    public String readLine() throws IOException {
	String result = null;
	StringBuffer line = new StringBuffer();
	int ch = 0;
	while(result == null && (ch = read()) != -1) {
	    switch(ch) {
	      case '\n':
		result = line.toString();
		break;

	      case '\r':
		setCheckpoint(1);
		if (read() != '\n') {
		    restoreCheckpoint();
		}
		result = line.toString();
		break;

	      default:
		line.append((char)(ch & 0xFF));
		break;
	    }
	}

	if (result == null) {
	    defuse();
	    isEOF = true;
	    if (line.length() > 0) {
		result = line.toString();
	    }
	}
	return result;
    }

    public void readFully(byte[] buff) throws IOException {
	readFully(buff, 0, buff.length);
    }

    public void readFully(byte[] buff, int offset, int len) throws IOException {
	int end = offset + len;
	for (int i=offset; i < end; i++) {
	    int ch = read();
	    if (ch == -1) {
		defuse();
		isEOF = true;
		throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_EOS));
	    } else {
		buff[i] = (byte)(ch & 0xFF);
	    }
	}
    }

    public String readUntil(String delim) throws IOException {
	StringBuffer sb = new StringBuffer();
	boolean found = false;
	do {
	    byte[] buff = readUntil((byte)delim.charAt(0));
	    if (buff == null) {
		return null;
	    }
	    sb.append(new String(buff));
	    setCheckpoint(delim.length());
	    byte[] b2 = new byte[delim.length()];
	    b2[0] = (byte)delim.charAt(0);
	    try {
		readFully(b2, 1, b2.length - 1);
		if (new String(b2).equals(delim)) {
		    found = true;
		} else {
		    sb.append((char)b2[0]);
		    restoreCheckpoint();
		}
	    } catch (EOFException e) {
		restoreCheckpoint();
		return readLine();
	    }
	} while(!found);
	return sb.toString();
    }

    public byte[] readUntil(int delim) throws IOException {
	int ch=0, len=0;
	byte[] buff = new byte[512];
	while((ch = read()) != -1 && ch != delim) {
	    if (len == buff.length) {
		byte[] old = buff;
		buff = new byte[old.length + 512];
		for (int i=0; i < old.length; i++) {
		    buff[i] = old[i];
		}
		old = null;
	    }
	    buff[len++] = (byte)(ch & 0xFF);
	}
	if (ch == -1 && len == 0) {
	    defuse();
	    isEOF = true;
	    return null;
	} else {
	    byte[] result = new byte[len];
	    for (int i=0; i < len; i++) {
		result[i] = buff[i];
	    }
	    return result;
	}
    }

    public int read(byte[] buff, int offset, int len) throws IOException {
	int bytesRead = 0;
	while (buffer.hasNext() && offset < buff.length) {
	    buff[offset++] = buffer.next();
	    bytesRead++;
	}
	bytesRead += in.read(buff, offset, len);
	int end = offset + bytesRead;
	for (int i=offset; buffer.hasCapacity() && i < end; i++) {
	    buffer.add((byte)(i & 0xFF));
	}
	reset();
	return bytesRead;
    }

    public int read() throws IOException {
	int i = -1;
	if (isEOF) {
	    throw new EOFException("PerishableReader");
	} else if (buffer.hasNext()) {
	    i = (int)buffer.next();
	} else {
	    i = in.read();
	    if (buffer.hasCapacity()) {
		buffer.add((byte)(i & 0xFF));
	    } else {
		buffer.clear(); // buffer overflow
	    }
	}
	if (i == -1) {
	    defuse();
	    isEOF = true;
	} else {
	    reset();
	}
	return i;
    }

    public void setCheckpoint(int readAheadLimit) throws IOException {
	buffer.init(readAheadLimit);
    }

    public void restoreCheckpoint() throws IOException {
	buffer.reset();
    }

    // Implement IPerishable

    public boolean checkExpired() {
	return expired;
    }

    public void setTimeout(long timeout) {
	if (timeout <= 0) {
	    this.timeout = 3600000L; // 1hr
	} else {
	    this.timeout = timeout;
	}
	reset();
    }

    public synchronized void reset() {
	defuse();
	task = new InterruptTask(Thread.currentThread());
	JOVALSystem.getTimer().schedule(task, timeout);
    }

    /**
     * Kill the scheduled interrupt task and purge it from the timer.
     */
    public void defuse() {
	if (task != null) {
	    task.cancel();
	    task = null;
	}
	JOVALSystem.getTimer().purge();
    }

    // Private

    protected PerishableReader(InputStream in, long timeout) {
	trace = Thread.currentThread().getStackTrace();
	logger = JOVALSystem.getLogger();
	this.in = in;
	setTimeout(timeout);
	isEOF = false;
	closed = false;
	expired = false;
	buffer = new Buffer(0);
	reset();
    }

    class InterruptTask extends TimerTask {
	Thread t;

	InterruptTask(Thread t) {
	    this.t = t;
	}

	public void run() {
	    if (PerishableReader.this.isEOF) {
		try {
		    PerishableReader.this.close();
		} catch (IOException e) {
		}
	    } else if (!closed && t.isAlive()) {
		t.interrupt();
		PerishableReader.this.expired = true;

		//
		// These can be a pain to debug, so we log the stack trace documenting the history of this reader.
		//
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < trace.length; i++) {
		    sb.append("\n");
		    if (i > 0) {
			sb.append("    at ");
		    }
		    sb.append(trace[i].getClassName()).append(".").append(trace[i].getMethodName());
		    if (i > 0) {
			sb.append(" ").append(trace[i].getFileName()).append(", line: ").append(trace[i].getLineNumber());
		    }
		}
		logger.warn(JOVALMsg.WARNING_PERISHABLEIO_INTERRUPT, sb.toString());
	    }
	    JOVALSystem.getTimer().purge();
	}
    }

    protected class Buffer {
	byte[] buff = null;
	int pos = 0;
	int len = 0;

	Buffer(int size) {
	    init(size);
	}

	void init(int size) {
	    buff = new byte[size];
	    len = 0;
	    pos = 0;
	}

	public void clear() {
	    buff = null;
	}

	public void reset() {
	    pos = 0;
	}

	public boolean hasNext() {
	    return buff != null && pos < len;
	}

	public byte next() throws NoSuchElementException {
	    if (hasNext()) {
		return buff[pos++];
	    } else {
		throw new NoSuchElementException();
	    }
	}

	public boolean hasCapacity() {
	    return buff != null && len < buff.length;
	}

	public void add(byte b) {
	    if (hasCapacity()) {
		buff[len++] = b;
		pos = len;
	    }
	}
    }
}
