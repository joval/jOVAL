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

/**
 * An IFile wrapper for a java.io.File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class FileProxy extends BaseFile {
    private File file;
    private String localName;

    FileProxy(IFilesystem fs, File file, String localName) {
	super(fs);
	this.file = file;
	if (localName.endsWith(fs.getDelimiter())) {
	    this.localName = localName.substring(0, localName.lastIndexOf(fs.getDelimiter()));
	} else {
	    this.localName = localName;
	}
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

    public boolean mkdir() {
	return file.mkdir();
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

    public IFile[] listFiles() throws IOException {
	String[] children = file.list();
	if (children == null) {
	    return new IFile[0];
	} else {
	    IFile[] files = new IFile[children.length];
	    for (int i=0; i < children.length; i++) {
		files[i] = fs.getFile(localName + fs.getDelimiter() + children[i]);
	    }
	    return files;
	}
    }

    public void delete() throws IOException {
	file.delete();
    }

    public String getLocalName() {
	return localName;
    }

    public String getName() {
	return file.getName();
    }

    public String toString() {
	return file.toString();
    }
}
