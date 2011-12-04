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
import java.util.HashSet;
import java.util.Vector;

import org.joval.intf.io.IReader;
import org.joval.intf.util.IPerishable;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Some stream utilities.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StreamTool {
    private static HashSet<PerishableReader>readers = new HashSet<PerishableReader>();
    private static IOTimer timer = new IOTimer();

    /**
     * Create a new IReader using the given InputStream and read timeout.  Instances created using this method are
     * periodically checked to see if they're blocking on a read beyond the pre-set expiration interval.  In that event,
     * the underlying stream is closed so that the blocking Thread can continue.
     *
     * @arg maxTime the maximum amount of time that should be allowed to elapse between successful reads, in milliseconds.
     *              If maxTime <= 0, the default of 1hr will apply.
     */
    public static IReader getSafeReader(InputStream in, long maxTime) {
	PerishableReader reader = new PerishableReader(in, maxTime);
	synchronized(readers) {
	    readers.add(reader);
	}
	synchronized(timer) {
	    if (!timer.running()) {
		timer.start();
	    }
	}
	return reader;
    }

    /**
     * Useful in debugging...
     */
    public static final void hexDump(byte[] buff, PrintStream out) {
	int numRows = buff.length / 16;
	if (buff.length % 16 > 0) numRows++; // partial row

	int ptr = 0;
	for (int i=0; i < numRows; i++) {
	    for (int j=0; j < 16; j++) {
		if (ptr < buff.length) {
		    if (j > 0) System.out.print(" ");
		    out.print(LittleEndian.toHexString(buff[ptr++]));
		} else {
		    break;
		}
	    }
	    out.println("");
	}
    }

    /**
     * Read from the stream until the buffer is full.
     */
    public static final void readFully(InputStream in, byte[] buff) throws IOException {
	for (int i=0; i < buff.length; i++) {
	    int ch = in.read();
	    if (ch == -1) {
	        throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_EOS));
	    } else {
	        buff[i] = (byte)(ch & 0xFF);
	    }
	}
    }

    /**
     * Copy from in to out in a Thread.  Returns the Thread.  Closes the InputStream when done, but not the OutputStream.
     */
    public static Thread copyInThread(InputStream in, OutputStream out) {
	Thread thread = new Thread(new Copier(in, out));
	thread.start();
	return thread;
    }

    /**
     * Copy completely from in to out.  Closes the InputStream when done, but not the OutputStream.
     */
    public static void copy(InputStream in, OutputStream out) {
	new Copier(in, out).run();
    }

    // Private

    private static class Copier implements Runnable {
	InputStream in;
	OutputStream out;

	Copier(InputStream in, OutputStream out) {
	    this.in = in;
	    this.out = out;
	}

	// Implement Runnable

	public void run() {
	    try {
		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) > 0) {
		    out.write(buff, 0, len);
		}
	    } catch (IOException e) {
	    } finally {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    private static class PerishableReader implements IReader, IPerishable {
	InputStream in;
	BufferedReader reader;
	boolean closed;
	long expires;
	long timeout;
	Thread thread;

	PerishableReader(InputStream in, long timeout) {
	    this.in = in;
	    setTimeout(timeout);

	    //
	    // If the underlying stream is actually a wrapper around another PerishableReader, which happens with the
	    // Sudo class, then overstamp its original timeout.
	    //
	    if (in instanceof IPerishable) {
		((IPerishable)in).setTimeout(this.timeout);
	    }
	    reader = new BufferedReader(new InputStreamReader(in));
	    closed = false;
	}

	// Implement IReader

	public synchronized void close() throws IOException {
	    if (!closed)  {
		closed = true;
		in.close();
		reader.close();
		readers.remove(this);
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

	// Internal

	void interrupt() {
	    if (thread != null) {
		thread.interrupt();
	    }
	}

	// Private

	private void reset() {
	    expires = System.currentTimeMillis() + timeout;

	    //
	    // Cause a reset if the underlying InputStream is another PerishableReader
	    //
	    if (in instanceof IPerishable) {
		((IPerishable)in).setTimeout(timeout);
	    }
	    thread = null;
	}
    }

    private static class IOTimer implements Runnable {
	Thread thread;
	boolean stop;

	public IOTimer() {
	    stop = true;
	}

	public void start() {
	    stop = false;
	    thread = new Thread(this);
	    thread.start();
	}

	public void stop() {
	    stop = true;
	}

	public boolean running() {
	    return !stop;
	}

	public void run() {
	    while (!stop) {
		Vector<PerishableReader> zombies = new Vector<PerishableReader>();

		synchronized(readers) {
		    for (PerishableReader reader : (HashSet<PerishableReader>)readers.clone()) {
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
		    for (PerishableReader zombie : zombies) {
			readers.remove(zombie);
		    }
		    if (readers.size() == 0) {
			stop = true;
			return;
		    }
		}
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	    }
	}
    }
}
