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
import java.util.Vector;

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
    private static Set<PerishableReader>readers = Collections.synchronizedSet(new HashSet<PerishableReader>());
    private static final IOTimer timer = new IOTimer();

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
	    synchronized(readers) {
		readers.add(reader);
	    }
	    synchronized(timer) {
		if (!timer.running()) {
		    timer.start();
		}
	    }
	}
	return reader;
    }

    private InputStream in;
    private BufferedReader reader;
    private boolean closed;
    private long expires;
    private long timeout;
    private Thread thread;

    // Implement IReader

    public void close() throws IOException {
	if (!closed)  {
	    closed = true;
	    in.close();
	    reader.close();
	    synchronized(readers) {
		readers.remove(this);
	    }
	}
    }

    public boolean checkClosed() {
	return closed;
    }

    public String readLine() throws IOException {
	thread = Thread.currentThread();
	String line = reader.readLine();
	reset();
	return line;
    }

    public void readFully(byte[] buff) throws IOException {
	thread = Thread.currentThread();
	for (int i=0; i < buff.length; i++) {
	    int ch = reader.read();
	    if (ch == -1) {
		throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_EOS));
	    } else {
		buff[i] = (byte)(ch & 0xFF);
	    }
	}
	reset();
    }

    public byte[] readUntil(int delim) throws IOException {
	thread = Thread.currentThread();
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
	thread = Thread.currentThread();
	int i = reader.read();
	reset();
	return i;
    }

    // Implement IPerishable

    public boolean checkExpired() {
	if (expires > 0) {
	    return expires <= System.currentTimeMillis();
	} else {
	    return false;
	}
    }

    public void setTimeout(long timeout) {
	if (timeout <= 0) {
	    this.timeout = 3600000L; // 1hr
	} else {
	    this.timeout = timeout;
	}
	expires = System.currentTimeMillis() + this.timeout;
    }

    public void reset() {
	expires = System.currentTimeMillis() + timeout;
	thread = null;
    }

    // Private

    private PerishableReader(InputStream in, long timeout) {
	this.in = in;
	setTimeout(timeout);
	reader = new BufferedReader(new InputStreamReader(in));
	closed = false;
    }

    private void interrupt() {
	if (thread != null) {
	    thread.interrupt();
	}
    }

    private static class IOTimer implements Runnable {
	Thread thread;
	boolean stop;

	public IOTimer() {
	    stop = true;
	}

	public synchronized void start() {
	    if (stop) {
		if (thread != null && thread.isAlive()) {
		    try {
			thread.join();
		    } catch (InterruptedException e) {
		    }
		}
		stop = false;
		thread = new Thread(this);
		thread.start();
	    }
	}

	public void stop() {
	    stop = true;
	}

	public boolean running() {
	    return !stop;
	}

	public void run() {
	    while (!stop) {
		Vector<PerishableReader>zombies = new Vector<PerishableReader>();

		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		synchronized(readers) {
		    try {
			for (PerishableReader reader : readers) {
			    if (reader.checkExpired()) {
				try {
				    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_READ_TIMEOUT, reader.timeout);
				    reader.interrupt();
				    reader.close();
				} catch (IOException e) {
				}
			    }
			    if (reader.checkClosed()) {
				zombies.add(reader);
			    }
			}
		    } catch (ConcurrentModificationException e) {
			JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		    for (PerishableReader zombie : zombies) {
			readers.remove(zombie);
		    }
		    if (readers.size() == 0) {
			stop = true;
		    }
		}
	    }
	}
    }
}
