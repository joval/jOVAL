// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

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
     */
    public static final void readFully(InputStream in, byte[] buff, long maxTime) throws IOException {
	TimedReader reader = new TimedReader(in, buff);
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
    }

    /**
     * Read from a stream until a '\n' is encountered, and return the String (minus the terminating '\n').
     */
    public static final String readLine(InputStream in) throws IOException {
	byte[] buff = readUntil(in, 10); // 10 == \n
	if (buff == null) {
	    return null;
	} else {
	    return new String(buff);
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

    static class TimedReader implements Runnable {
	InputStream in;
	byte[] buff;
	Thread t;
	boolean finished;
	IOException error = null;

	TimedReader(InputStream in, byte[] buff) {
	    finished = false;
	    this.in = in;
	    this.buff = buff;
	    t = new Thread(this);
	}

	public void start() {
	    t.start();
	}

	public boolean isFinished() {
	    return finished;
	}

	public boolean hasError() {
	    return error != null;
	}

	public IOException getError() {
	    return error;
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

	public void run() {
	    try {
		StreamTool.readFully(in, buff);
	    } catch (IOException e) {
		error = e;
	    }
	    finished = true;
	}
    }
}
