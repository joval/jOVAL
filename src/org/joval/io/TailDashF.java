// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IRandomAccess;

/**
 * A utility to stream the appended contents of a file as it is polled.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TailDashF implements Runnable {
    private IFile file;
    private PipedInputStream in;
    private Thread thread;

    public TailDashF(IFile file) {
	this.file = file;
	in = new PipedInputStream();
	thread = new Thread(this, "tail -f " + file.toString());
    }

    public TailDashF(IFile file, ThreadGroup group) {
	this.file = file;
	in = new PipedInputStream();
	thread = new Thread(group, this, "tail -f " + file.toString());
    }

    public InputStream getInputStream() {
	return in;
    }

    public void start() {
	thread.start();
    }

    public void join() throws InterruptedException {
	join(0);
    }

    public void join(long millis) throws InterruptedException {
	thread.join(millis);
    }

    public void interrupt() {
	thread.interrupt();
	try {
	    in.close();
	} catch (IOException e) {
	}
    }

    public boolean isAlive() {
	return thread.isAlive();
    }

    // Implement Runnable

    public void run() {
	PipedOutputStream out = null;
	IRandomAccess ra = null;
	try {
	    out = new PipedOutputStream(in);
	    long lastLen = 0;
	    while (file.exists()) {
		ra = file.getRandomAccess("r");
		long len = file.length();
		if (len > lastLen) {
		    ra.seek(lastLen);
		    byte[] buff = new byte[(int)(len - lastLen)];
		    lastLen = len;
		    ra.readFully(buff);
		    out.write(buff);
		    out.flush();
		}
		ra.close();
		ra = null;
		Thread.sleep(100);
	    }
	} catch (IOException e) {
	} catch (InterruptedException e) {
	} finally {
	    if (ra != null) {
		try {
		    ra.close();
		} catch (IOException e) {
		}
	    }
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
