// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.tree.INode;

/**
 * An IFile wrapper for a java.io.File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class FileProxy extends BaseFile {
    private File file;

    FileProxy(IFilesystem fs, File file) {
	super(fs);
	this.file = file;
    }

    File getFile() {
	return file;
    }

    // Implement methods left abstract in BaseFile

    public String getCanonicalPath() {
	try {
	    return file.getCanonicalPath();
	} catch (IOException e) {
	    return file.getPath();
	}
    }

    // Implement IFile

    /**
     * Not really supported in this implementation.
     */
    public long accessTime() throws IOException {
	return lastModified();
    }

    public long createTime() throws IOException {
	return file.lastModified();
    }

    public boolean exists() throws IOException {
	return file.exists();
    }

    public InputStream getInputStream() throws IOException {
	return new FileInputStream(file);
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	return new FileOutputStream(file, append);
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return new RandomAccessFileProxy(new RandomAccessFile(file, mode));
    }

    public boolean isDirectory() throws IOException {
	return file.isDirectory();
    }

    public boolean isFile() throws IOException {
	return file.isFile();
    }

    public boolean isLink() throws IOException {
	return !file.getPath().equals(file.getCanonicalPath());
    }

    public long lastModified() throws IOException {
	return file.lastModified();
    }

    public long length() throws IOException {
	return file.length();
    }

    public String[] list() throws IOException {
	String[] children = file.list();
	if (children == null) {
	    return new String[0];
	} else {
	    return children;
	}
    }

    public int getFileType() throws IOException {
	return FILE_TYPE_DISK;
    }

    public void delete() throws IOException {
	file.delete();
    }

    public String getLocalName() {
	return toString();
    }

    public String getName() {
	return file.getName();
    }

    public String toString() {
	return file.toString();
    }
}
