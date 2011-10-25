// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Provides a facade for an InputStream while logging its contents to a file.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StreamLogger extends InputStream implements Runnable {
    private Thread t;
    private InputStream in;
    private File outLog;
    private String comment;
    private int pos, len, triggerLen;
    private byte[] buff;
    private boolean forceClosed = false;

    public StreamLogger(InputStream in, File outLog) throws IOException {
	this(null, in, outLog);
    }

    public StreamLogger(String comment, InputStream in, File outLog) throws IOException {
	this.comment = comment;
	this.in = in;
	this.outLog = outLog;
	buff = new byte[1024];
	triggerLen = 1023;
	pos = 0;
	len = 0;


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
		add(ch);
	    }
	} catch (IOException e) {
	    if (!forceClosed) {
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_IO, in.getClass().getName(), e.getMessage());
	    }
	}
    }

    public int read() throws IOException {
	if (pos < len) {
	    return buff[pos++];
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
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_STREAMLOGGER_CLOSE, outLog);
	    forceClosed = true;
	    t.interrupt();
	}
	if (len > 0) {
	    save();
	}
    }

    // Private

    private void add(int ch) {
	if (len == triggerLen) {
	    int newLen = buff.length + 1024;
	    byte[] old = buff;
	    buff = new byte[newLen];
	    for (int i=0; i < old.length; i++) {
		buff[i] = old[i];
	    }
	    old = null;
	    triggerLen = newLen - 1;
	}
	buff[len++] = (byte)ch;
    }

    private void save() {
	OutputStream out = null;
	try {
	    out = new FileOutputStream(outLog);
	    if (comment != null) {
		StringBuffer sb = new StringBuffer("# ");
		sb.append(comment);
		sb.append(System.getProperty("line.separator"));
		out.write(sb.toString().getBytes());
	    }
	    out.write(buff, 0, len);
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_IO, outLog, e.getMessage());
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
