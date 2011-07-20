// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.smb;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.smb.SmbRandomAccessFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IRandomAccess;

/**
 * An IFile wrapper for an SmbFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmbFileProxy implements IFile {
    private SmbFile smbFile;
    private String localName;

    SmbFileProxy(SmbFile smbFile, String localName) {
	this.smbFile = smbFile;
	this.localName = localName;
    }

    SmbFile getSmbFile() {
	return smbFile;
    }

    // Implement IFile

    /**
     * Does nothing in this implementation.
     */
    public void close() throws IOException {
    }

    /**
     * Not really supported by this implementation.
     */
    public long accessTime() throws IOException {
	return lastModified();
    }

    public long createTime() throws IOException {
	return smbFile.createTime();
    }

    public boolean exists() throws IOException {
	return smbFile.exists();
    }

    public InputStream getInputStream() throws IOException {
	return new SmbFileInputStream(smbFile);
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	return new SmbFileOutputStream(smbFile, append);
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return new SmbRandomAccessProxy(new SmbRandomAccessFile(smbFile, mode));
    }

    public boolean isDirectory() throws IOException {
	return smbFile.isDirectory();
    }

    public boolean isFile() throws IOException {
	return smbFile.isFile();
    }

    public long lastModified() throws IOException {
	return smbFile.lastModified();
    }

    public long length() throws IOException {
	return smbFile.length();
    }

    public String[] list() throws IOException {
	return smbFile.list();
    }

    public int getType() throws IOException {
	switch(smbFile.getType()) {
	  case SmbFile.TYPE_FILESYSTEM:
	    return FILE_TYPE_DISK;
	  case SmbFile.TYPE_WORKGROUP:
	  case SmbFile.TYPE_SERVER:
	  case SmbFile.TYPE_SHARE:
	    return FILE_TYPE_REMOTE;
	  case SmbFile.TYPE_NAMED_PIPE:
	    return FILE_TYPE_PIPE;
	  case SmbFile.TYPE_PRINTER:
	  case SmbFile.TYPE_COMM:
	    return FILE_TYPE_CHAR;
	  default:
	    return FILE_TYPE_UNKNOWN;
	}
    }

    public void delete() throws IOException {
	smbFile.delete();
    }

    public String getLocalName() {
	return localName;
    }

    public String toString() {
	return smbFile.getUncPath();
    }
}
