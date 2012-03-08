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
 * An IFile implementation based on a regular Java File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileProxy extends BaseFile {
    private BaseFilesystem fs;
    private File file;

    public boolean isfile = false;
    public boolean isdir = false;

    /**
     * Create a file proxy based on a File object.
     */
    public FileProxy(BaseFilesystem fs, File file, String path) {
	super(fs, path);
	accessible = true;
	this.file = file;
    }

    public FileProxy(BaseFilesystem fs, String path) {
	super(fs, path);
    }

    public File getFile() {
	if (file == null) {
	    accessible = true;
	    file = new File(path);
	}
	return file;
    }

    // Implement ICacheable

    public boolean isLink() {
	if (accessible) {
	    try {
		File canon;
		if (file.getParent() == null) {
		    canon = file;
		} else {
		    File canonDir = file.getParentFile().getCanonicalFile();
		    canon = new File(canonDir, file.getName());
		}
		return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
	    } catch (IOException e) {
		return false;
	    }
	} else {
	    return false;	
	}
    }

    @Override
    public String getLinkPath() throws IllegalStateException {
	if (isLink()) {
	    return getCanonicalPath();
	} else {
	    return super.getLinkPath(); // throw exception
	}
    }

    // Implement IFile

    public String getCanonicalPath() {
	if (accessible) {
	    try {
		return file.getCanonicalPath();
	    } catch (IOException e) {
		return path;
	    }
	} else {
	    return path;
	}
    }

    /**
     * Not really supported in this implementation.
     */
    public long accessTime() throws IOException {
	return lastModified();
    }

    public long createTime() throws IOException {
	return getFile().lastModified();
    }

    public boolean exists() throws IOException {
	if (accessible) {
	    return file.exists();
	} else {
	    return true;
	}
    }

    public boolean mkdir() {
	return getFile().mkdir();
    }

    public InputStream getInputStream() throws IOException {
	return new FileInputStream(getFile());
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	return new FileOutputStream(getFile(), append);
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return new RandomAccessFileProxy(new RandomAccessFile(getFile(), mode));
    }

    public boolean isDirectory() throws IOException {
	if (accessible) {
	    return file.isDirectory();
	} else { 
	    return isdir;
	}
    }

    public boolean isFile() throws IOException {
	if (accessible) {
	    return file.isFile();
	} else {
	    return isfile;
	}
    }

    public long lastModified() throws IOException {
	return getFile().lastModified();
    }

    public long length() throws IOException {
	return getFile().length();
    }

    public void delete() throws IOException {
	getFile().delete();
    }

    public String toString() {
	if (accessible) {
	    return file.toString();
	} else {
	    return path;
	}
    }

    // Internal
 
    class RandomAccessFileProxy implements IRandomAccess {
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
}
