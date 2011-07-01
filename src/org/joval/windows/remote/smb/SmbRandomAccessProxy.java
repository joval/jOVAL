// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.smb;

import java.io.IOException;
import java.io.EOFException;

import jcifs.smb.SmbRandomAccessFile;

import org.joval.intf.io.IRandomAccess;

/**
 * An IRandomAccess wrapper for an SmbRandomAccessFile.  This wrapper corrects for the "drift" in the file
 * pointer of an SmbRandomAccessFile that makes it impossible to make any assumptions about where it might be
 * pointing after you perform a read operation on it.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmbRandomAccessProxy implements IRandomAccess {
    private SmbRandomAccessFile smb;
    private long ptr;

    SmbRandomAccessProxy(SmbRandomAccessFile smb) {
	this.smb = smb;
	ptr = 0;
    }

    // Implement IRandomAccess

    public void readFully(byte[] buff) throws IOException {
	smb.readFully(buff);
	ptr += buff.length;
	if (getFilePointer() != ptr) {
	    seek(ptr);
	}
    }

    public void close() throws IOException {
	smb.close();
    }

    public void seek(long pos) throws IOException {
	ptr = pos;
	smb.seek(pos);
    }

    public int read() throws IOException {
	int i = smb.read();
	ptr++;
	if (getFilePointer() != ptr) {
	    seek(ptr);
	}
	return i;
    }

    public int read(byte[] buff) throws IOException {
	return read(buff, 0, buff.length);
    }

    public int read(byte[] buff, int offset, int len) throws IOException {
	int i = smb.read(buff, offset, len);
	ptr += i;
	if (getFilePointer() != ptr) {
	    seek(ptr);
	}
	return i;
    }

    public long length() throws IOException {
	return smb.length();
    }

    public long getFilePointer() throws IOException {
	return smb.getFilePointer();
    }
}
