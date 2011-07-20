// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix.remote.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.SftpATTRS;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IRandomAccess;

/**
 * An IFile wrapper for an SFTP channel.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SftpFile implements IFile {
    private ChannelSftp cs;
    private SftpATTRS attrs = null;
    private String path;
    boolean tested=false, doesExist;

    SftpFile(ChannelSftp cs, String path) throws JSchException {
	cs.connect();
	this.cs = cs;
	this.path = path;
    }

    // Implement IFile

    public void close() throws IOException {
	try {
	    if (cs.isConnected()) {
		cs.disconnect();
	    }
	} catch (Throwable e) {
	    throw new IOException(e);
	}
    }

    public long accessTime() throws IOException {
	if (exists()) {
	    return attrs.getAccessTime();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    /**
     * Not really supported by this implementation.
     */
    public long createTime() throws IOException {
	return lastModified();
    }

    public boolean exists() throws IOException {
	if (!tested) {
	    tested = true;
	    try {
		attrs = cs.lstat(path);
		doesExist = true;
	    } catch (SftpException e) {
		switch(getErrorCode(e)) {
		  case SftpError.NO_SUCH_FILE:
		    doesExist = false;
		    break;
		  default:
		    throw new IOException(e);
		}
	    }
	}
	return doesExist;
    }

    public InputStream getInputStream() throws IOException {
	if (exists()) {
	    try {
		return cs.get(path);
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	if (isFile()) {
	    try {
		return cs.put(path);
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	} else {
	    throw new IOException("Cannot write to a directory: " + path);
	}
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isDirectory() throws IOException {
	if (exists()) {
	    return attrs.isDir();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean isFile() throws IOException {
	if (exists()) {
	    return !isDirectory();
	} else {
	    return true;
	}
    }

    public long lastModified() throws IOException {
	if (exists()) {
	    return attrs.getModifiedTime();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public long length() throws IOException {
	if (exists()) {
	    return attrs.getSize();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public String[] list() throws IOException {
	try {
	    List<ChannelSftp.LsEntry> list = cs.ls(path);
	    String[] children = new String[0];
	    ArrayList<String> al = new ArrayList<String>();
	    Iterator<ChannelSftp.LsEntry> iter = list.iterator();
	    while (iter.hasNext()) {
		ChannelSftp.LsEntry entry = iter.next();
		if (!".".equals(entry.getFilename()) && !"..".equals(entry.getFilename())) {
		    al.add(entry.getFilename());
		}
	    }
	    return al.toArray(children);
	} catch (SftpException e) {
	    throw new IOException(e);
	}
    }

    public int getType() throws IOException {
	return FILE_TYPE_DISK;
    }

    public void delete() throws IOException {
	if (exists()) {
	    try {
		cs.rm(path);
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public String getLocalName() {
	return path;
    }

    public String toString() {
	try {
	    return cs.realpath(path);
	} catch (SftpException e) {
	    return path;
	}
    }

    // Private

    private int getErrorCode(SftpException e) {
	try {
	    String s = e.toString();
	    return Integer.parseInt(s.substring(0, s.indexOf(":")));
	} catch (Exception ex) {
	    return -1;
	}
    }
}
