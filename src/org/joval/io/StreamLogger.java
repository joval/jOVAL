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
public class StreamLogger extends InputStream {
    private InputStream in;
    private OutputStream out;
    private boolean closed = false;

    public StreamLogger(InputStream in, File outLog) throws IOException {
	this(null, in, outLog);
    }

    public StreamLogger(String comment, InputStream in, File outLog) throws IOException {
	this(comment, in, new FileOutputStream(outLog));
    }

    public StreamLogger(String comment, InputStream in, OutputStream out) throws IOException {
	this.in = in;
	this.out = out;
	if (comment != null) {
	    StringBuffer sb = new StringBuffer("# ");
	    sb.append(comment);
	    sb.append(System.getProperty("line.separator"));
	    out.write(sb.toString().getBytes());
	}
    }

    public InputStream getInputStream() {
	return in;
    }

    public int read() throws IOException {
	int ch = in.read();
	if (ch != -1) {
	    out.write(ch);
	}
	return ch;
    }

    public int read(byte[] buff) throws IOException {
	return read(buff, 0, buff.length);
    }

    public int read(byte[] buff, int offset, int len) throws IOException {
	int bytesRead = in.read(buff, offset, len);
	if (bytesRead > 0) {
	    out.write(buff, offset, bytesRead);
	}
	return bytesRead;
    }

    public long skip(long n) throws IOException {
	return in.skip(n);
    }

    public int available() throws IOException {
	return in.available();
    }

    public void close() throws IOException {
	if (closed) {
	    return;
	}

	IOException ex = null;
	try {
	    in.close();
	} catch (IOException e) {
	    ex = e;
	}
	try {
	    out.close();
	} catch (IOException e) {
	    // fail silently
	}
	closed = true;
	if (ex != null) {
	    throw ex;
	}
    }

    public void mark(int readLimit) {
	in.mark(readLimit);
    }

    public void reset() throws IOException {
	in.reset();
    }

    public boolean markSupported() {
	return in.markSupported();
    }

}
