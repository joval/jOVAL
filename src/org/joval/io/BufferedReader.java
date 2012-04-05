// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IReader;

/**
 * An IReader backed by a BufferedReader.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BufferedReader implements IReader {
    private java.io.BufferedReader reader;
    private boolean open, eof;

    public BufferedReader(InputStream in) throws IOException {
	open = true;
	eof = false;
	// Use ASCII encoding so that a char is a byte
	this.reader = new java.io.BufferedReader(new InputStreamReader(in, "US-ASCII"));
    }

    // Implement IReader

    public int read() throws IOException {
	return reader.read();
    }

    public String readLine() throws IOException {
	return reader.readLine();
    }

    public void readFully(byte[] buff) throws IOException {
	int ptr = 0;
	while (ptr < buff.length) {
	    char[] cbuff = new char[buff.length];
	    int len = reader.read(cbuff, ptr, buff.length - ptr);
	    if (len == -1) {
		eof = true;
		throw new EOFException("EOF");
	    } else {
		for (int i=0; i < len; i++) {
		    buff[i] = (byte)(cbuff[i] & 0xFF);
		}
		ptr += len;
	    }
	}
    }

    public byte[] readUntil(int ch) throws IOException {
	ByteArrayOutputStream memory = new ByteArrayOutputStream();
	char[] cbuff = new char[512];
	byte[] buff = new byte[512];
	int len = 0;
	while ((len = reader.read(cbuff)) > 0) {
	    if (len == -1) {
		eof = true;
		throw new EOFException("EOF");
	    } else {
		for (int i=0; i < len; i++) {
		    if (i == ch) {
			if (i > 0) {
			    memory.write(buff, 0, i - 1);
			}
			return memory.toByteArray();
		    } else {
			buff[i] = (byte)(cbuff[i] & 0xFF);
		    }
		}
		memory.write(buff, 0, len);
	    }
	}
	throw new IOException("read failed");
    }

    public void close() throws IOException {
	reader.close();
    }

    public boolean checkClosed() {
	return !open;
    }

    public boolean checkEOF() {
	return eof;
    }

    public void setCheckpoint(int readAheadLimit) throws IOException {
	reader.mark(readAheadLimit);
    }

    public void restoreCheckpoint() throws IOException {
	reader.reset();
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger){}

    public LocLogger getLogger() {
	return null;
    }    
}
