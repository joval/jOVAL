// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Provides a facade for an InputStream while logging its contents to a file.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StreamLogger extends InputStream implements Runnable {
    private Thread t;
    private InputStream in;
    private OutputStream out;
    private RandomAccessFile raf;
    private long pos = 0;
    private long len = 0;

    public StreamLogger (InputStream in, File outLog) throws IOException {
	this.in = in;
	out = new FileOutputStream(outLog);
	raf = new RandomAccessFile(outLog, "r");
    }

    public void start() {
	t = new Thread(this);
	t.start();
    }

    public void join() throws InterruptedException {
	t.join();
    }

    public void run() {
	try {
	    int ch;
	    while((ch = in.read()) != -1) {
		out.write(ch);
		len++;
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public int read() throws IOException {
	if (pos < len) {
	    int ch = raf.read();
	    pos++;
	    return ch;
	} else if (t.isAlive()) {
	    try {
		Thread.sleep(200);
	    } catch (InterruptedException e) {
	    }
	    return read();
	} else {
	    return -1;
	}
    }

    public void close() throws IOException {
	if (t.isAlive()) {
	    t.interrupt();
	}
	out.close();
	raf.close();
    }
}
