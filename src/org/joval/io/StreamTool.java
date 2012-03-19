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

/**
 * Some stream utilities.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StreamTool {
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
		    String iHex = Integer.toHexString((int)buff[ptr++]);
		    if (iHex.length() == 0) {
			out.print("0");
		    }
		    out.print(iHex);
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
	        throw new EOFException(JOVALMsg.getMessage(JOVALMsg.ERROR_EOS));
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
}
