// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ConcurrentModificationException;
import java.util.Collections;
import java.util.HashSet;
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

    private InputStream in;
    private BufferedReader reader;
    private boolean isEOF, closed, expired;
    private long timeout;
    private TimerTask task;
    private LocLogger logger;

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
	    if (task != null) {
		task.cancel();
		task = null;
	    }
	    in.close();
	    reader.close();
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
	String line = reader.readLine();
	if (line == null) {
	    isEOF = true;
	} else {
	    reset();
	}
	return line;
    }

    public void readFully(byte[] buff) throws IOException {
	for (int i=0; i < buff.length; i++) {
	    int ch = reader.read();
	    if (ch == -1) {
		isEOF = true;
		throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_EOS));
	    } else {
		buff[i] = (byte)(ch & 0xFF);
	    }
	}
	reset();
    }

    public byte[] readUntil(int delim) throws IOException {
	int ch=0, len=0;
	byte[] buff = new byte[512];
	while((ch = reader.read()) != -1 && ch != delim) {
	    if (len == buff.length) {
		byte[] old = buff;
		buff = new byte[old.length + 512];
		for (int i=0; i < old.length; i++) {
		    buff[i] = old[i];
		}
		old = null;
	    }
	    buff[len++] = (byte)ch;
	}
	if (ch == -1 && len == 0) {
	    isEOF = true;
	    return null;
	} else {
	    byte[] result = new byte[len];
	    for (int i=0; i < len; i++) {
		result[i] = buff[i];
	    }
	    reset();
	    return result;
	}
    }

    public int read() throws IOException {
	int i = reader.read();
	if (i == -1) {
	    isEOF = true;
	} else {
	    reset();
	}
	return i;
    }

    public void setCheckpoint(int readAheadLimit) throws IOException {
	reader.mark(readAheadLimit);
    }

    public void restoreCheckpoint() throws IOException {
	reader.reset();
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
	if (task != null) {
	    task.cancel();
	}
	task = new InterruptTask(Thread.currentThread());
	JOVALSystem.getTimer().schedule(task, timeout);
    }

    // Private

    private PerishableReader(InputStream in, long timeout) {
	this.in = in;
	setTimeout(timeout);
	reader = new BufferedReader(new InputStreamReader(in));
	isEOF = false;
	closed = false;
	expired = false;
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
	    } else if (t.isAlive()) {
		t.interrupt();
		PerishableReader.this.expired = true;
	    }
	    JOVALSystem.getTimer().purge();
	}
    }
}
