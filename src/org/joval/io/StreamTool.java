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

import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Some stream utilities.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StreamTool {
    /**
     * Read from the InputStream until the buffer is completely filled.
     *
     * @throws EOFException if the end of the stream is reached before the buffer is full.
     */
    public static final void readFully(InputStream in, byte[] buff) throws IOException {
	for (int offset=0, read=0; read < buff.length; offset = read) {
	    int len = in.read(buff, offset, buff.length);
	    if (len == -1) {
		throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_EOS));
	    } else {
		read += len;
	    }
	}
    }

    /**
     * Read from the InputStream until the buffer is completely filled, but take no more than maxTime milliseconds
     * to do so.  If the timeout expires, the InputStream is closed and an EOFException will be thrown.
     *
     * @arg maxTime set to 0 to wait potentially forever
     */
    public static final void readFully(InputStream in, byte[] buff, long maxTime) throws IOException {
	if (maxTime > 0) {
	    TimedFullReader reader = new TimedFullReader(in, buff);
	    reader.start();
	    long end = System.currentTimeMillis() + maxTime;
	    while (!reader.isFinished() && System.currentTimeMillis() < end) {
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	    }
	    if (reader.hasError()) {
		throw reader.getError();
	    } else if (!reader.isFinished()) {
		reader.cancel();
		throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_READ_TIMEOUT, maxTime));
	    }
	} else {
	    readFully(in, buff);
	}
    }

    /**
     * Read from a stream until a '\n' is encountered, and return the String (minus the terminating '\n').
     */
    public static final String readLine(InputStream in) throws IOException {
	byte[] buff = readUntil(in, '\n');
	if (buff == null) {
	    return null;
	} else {
	    return new String(buff);
	}
    }

    /**
     * Read a line from the InputStream until a newline character is reached (or the stream is closed or otherwise ends),
     * but take no more than maxTime milliseconds to do so.  If the timeout expires, the InputStream is closed and an
     * EOFException will be thrown.
     *
     * @arg maxTime set to 0 to wait potentially forever
     */
    public static final String readLine(InputStream in, long maxTime) throws IOException {
	if (maxTime > 0) {
	    TimedLineReader reader = new TimedLineReader(in);
	    reader.start();
	    long end = System.currentTimeMillis() + maxTime;
	    while (!reader.isFinished() && System.currentTimeMillis() < end) {
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	    }
	    if (reader.hasError()) {
		throw reader.getError();
	    } else if (reader.isFinished()) {
		try {
		    reader.join();
		} catch (InterruptedException e) {
		}
		return reader.getResult();
	    } else {
		reader.cancel();
		throw new EOFException(JOVALSystem.getMessage(JOVALMsg.ERROR_READ_TIMEOUT, maxTime));
	    }
	} else {
	    return readLine(in);
	}
    }

    /**
     * Create a new ManagedReader using the given InputStream and read timeout.
     *
     * @arg maxTime the maximum time between successful reads, in milliseconds.  If <=0, the default of 1hr will apply.
     */
    public static synchronized ManagedReader getManagedReader(InputStream in, long maxTime) {
	ManagedReader reader = new ManagedReader(in, maxTime);
	readers.add(reader);
	if (!timer.running()) {
	    timer.start();
	}
	return reader;
    }

    /**
     * A ManagedReader is a stream reader that is periodically checked to see if it's blocking on a read beyond the
     * pre-set expiration interval.  In that event, the underlying stream is closed so that the blocking Thread can continue.
     *
     * All managed readers are managed by a timer/watcher in the StreamTool class.
     */
    public static class ManagedReader {
	InputStream in;
	BufferedReader reader;
	boolean closed;
	long expires;
	long timeout;

	ManagedReader(InputStream in, long timeout) {
	    this.in = in;
	    if (timeout <= 0) {
		this.timeout = 3600000L; // 1hr
	    } else {
		this.timeout = timeout;
	    }
	    reader = new BufferedReader(new InputStreamReader(in));
	    closed = false;
	}

	/**
	 * Close the stream.  This removed the reader from the manager.
	 */
	public synchronized void close() throws IOException {
	    if (!closed)  {
		closed = true;
		in.close();
		reader.close();
		readers.remove(this);
	    }
	}

	/**
	 * Read the next line of text from the input.
	 */
	public String readLine() throws IOException {
	    String line = reader.readLine();
	    reset();
	    return line;
	}

	// Internal

	void reset() {
	    expires = System.currentTimeMillis() + timeout;
	}

	boolean checkClosed() {
	    return closed;
	}

	boolean checkExpired() {
	    if (expires > 0) {
		return expires <= System.currentTimeMillis();
	    } else {
		return false;
	    }
	}
    }

    /**
     * Read from a stream until the specified delimiter is encountered, and return the bytes.
     */
    public static final byte[] readUntil(InputStream in, int delim) throws IOException {
	int ch=0, len=0;
	byte[] buff = new byte[512];
	while((ch = in.read()) != -1 && ch != delim) {
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
	    return result;
	}
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
     * Copy from in to out in a Thread.  Returns the Thread.  Closes the InputStream when done, but not the OutputStream.
     */
    public static Thread copyInThread(InputStream in, OutputStream out) {
	Thread thread = new Thread(new Copier(in, out));
	thread.start();
	return thread;
    }

    // Internal

    static class Copier implements Runnable {
	InputStream in;
	OutputStream out;

	Copier(InputStream in, OutputStream out) {
	    this.in = in;
	    this.out = out;
	}

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

    static abstract class TimedReader implements Runnable {
	InputStream in;
	boolean finished;
	String result;
	IOException error;
	Thread t;

	TimedReader() {
	    finished = false;
	    result = null;
	    error = null;
	    t = new Thread(this);
	}

	TimedReader(InputStream in) {
	    this();
	    this.in = in;
	}

	void start() {
	    t.start();
	}

	boolean isFinished() {
	    return finished;
	}

	boolean hasError() {
	    return error != null;
	}

	public synchronized void cancel() {
	    if (!finished) {
		try {
		    in.close();
		} catch (IOException e) {
		    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	    finished = true;
	}

	public String getResult() {
	    return result;
	}

	public IOException getError() {
	    return error;
	}

	public void join() throws InterruptedException {
	    t.join();
	}
    }

    static class TimedFullReader extends TimedReader {
	byte[] buff;

	TimedFullReader(InputStream in, byte[] buff) {
	    super(in);
	    this.buff = buff;
	}

	public void run() {
	    try {
		StreamTool.readFully(in, buff);
	    } catch (IOException e) {
		error = e;
	    }
	    finished = true;
	}
    }

    static class TimedLineReader extends TimedReader {
	TimedLineReader(InputStream in) {
	    super(in);
	}

	public void run() {
	    try {
		result = StreamTool.readLine(in);
	    } catch (IOException e) {
		error = e;
	    }
	    finished = true;
	}
    }

    private static HashSet<ManagedReader>readers = new HashSet<ManagedReader>();
    private static IOTimer timer = new IOTimer();

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
		Vector<ManagedReader> zombies = new Vector<ManagedReader>();
		for (ManagedReader reader : readers) {
		    if (reader.checkExpired()) {
			try {
System.out.println(">>>>>>>>> EXPIRED READER <<<<<<<<<<<<");
			    reader.close();
			} catch (IOException e) {
			}
		    }
		    if (reader.checkClosed()) {
			zombies.add(reader);
		    }
		}
		for (ManagedReader zombie : zombies) {
		    readers.remove(zombie);
		}
		if (readers.size() == 0) {
		    stop = true;
		} else {
		    try {
			Thread.sleep(1000);
		    } catch (InterruptedException e) {
		    }
		}
	    }
	}
    }
}
