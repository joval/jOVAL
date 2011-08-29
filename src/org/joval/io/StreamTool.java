// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.*;

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
		throw new EOFException(JOVALSystem.getMessage("ERROR_EOS"));
	    } else {
		read += len;
	    }
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
}
