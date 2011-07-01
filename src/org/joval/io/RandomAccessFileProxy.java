// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.IOException;
import java.io.EOFException;
import java.io.RandomAccessFile;

import org.joval.intf.io.IRandomAccess;

/**
 * An IRandomAccess wrapper for a RandomAccessFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RandomAccessFileProxy implements IRandomAccess {
    private RandomAccessFile raf;

    public RandomAccessFileProxy(RandomAccessFile raf) {
	this.raf = raf;
    }

    // Implement IRandomAccess

    public void readFully(byte[] buff) throws IOException {
	raf.readFully(buff);
    }

    public void close() throws IOException {
	raf.close();
    }

    public void seek(long pos) throws IOException {
	raf.seek(pos);
    }

    public int read() throws IOException {
	return raf.read();
    }

    public int read(byte[] buff) throws IOException {
	return raf.read(buff);
    }

    public int read(byte[] buff, int offset, int len) throws IOException {
	return raf.read(buff, offset, len);
    }

    public long length() throws IOException {
	return raf.length();
    }

    public long getFilePointer() throws IOException {
	return raf.getFilePointer();
    }
}
