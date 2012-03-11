// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;

/**
 * A CacheFile that works with Java File objects, on the local machine.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DefaultFile extends CacheFile {
    protected DefaultFile(CacheFilesystem fs, String path) {
	super(fs, path);
    }

    /**
     * Create a file proxy based on a File object.
     */
    public DefaultFile(CacheFilesystem fs, File file, String path) throws IOException {
	super(fs, path);
	accessor = new DefaultAccessor(file);
	try {
	    info = accessor.getInfo();
	} catch (FileNotFoundException e) {
	}
    }

    /**
     * Create a file proxy based on information.
     */
    public DefaultFile(CacheFilesystem fs, FileInfo info, String path) {
	super(fs, path);
	this.info = info;
    }

    @Override
    public String toString() {
	if (accessor == null) {
	    return path;
	} else {
	    return accessor.toString();
	}
    }

    // Implement abstract methods from CacheFile

    public FileAccessor getAccessor() {
	if (accessor == null) {
	    accessor = new DefaultAccessor(new File(path));
	}
	return accessor;
    }

    // Internal

    protected class DefaultAccessor extends FileAccessor {
	private File file;

	protected DefaultAccessor(File file) {
	    this.file = file;
	}

	public String toString() {
	    return file.toString();
	}

	// Implement abstract methods from FileAccessor

	public boolean exists() {
	    return file.exists();
	}

	public void delete() {
	    file.delete();
	}

	public FileInfo getInfo() throws IOException {
	    if (exists()) {
		long ctime = getCtime();
		long mtime = getMtime();
		long atime = getAtime();
		long length = getLength();

		// Determine the file type...

		FileInfo.Type type = FileInfo.Type.FILE;

		File canon;
		if (file.getParent() == null) {
		    canon = file;
		} else {
		    File canonDir = file.getParentFile().getCanonicalFile();
		    canon = new File(canonDir, file.getName());
		}

		if (!canon.getCanonicalFile().equals(canon.getAbsoluteFile())) {
		    type = FileInfo.Type.LINK;
		} else if (file.isDirectory()) {
		    type = FileInfo.Type.DIRECTORY;
		}

		return new FileInfo(ctime, mtime, atime, type, length);
	    } else {
		throw new FileNotFoundException(path);
	    }
	}

	public long getCtime() throws IOException {
	    return FileInfo.UNKNOWN_TIME;
	}

	public long getMtime() throws IOException {
	    return file.lastModified();
	}

	public long getAtime() throws IOException {
	    return FileInfo.UNKNOWN_TIME;
	}

	public long getLength() throws IOException {
	    return file.length();
	}

	public IRandomAccess getRandomAccess(String mode) throws IOException {
	    return new RandomAccessImpl(new RandomAccessFile(file, mode));
	}

	public InputStream getInputStream() throws IOException {
	    return new FileInputStream(file);
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
	    return new FileOutputStream(file, append);
	}

	public String getCanonicalPath() throws IOException {
	    return file.getCanonicalPath();
	}

	public String[] list() throws IOException {
	    return file.list();
	}

	public boolean mkdir() {
	    return file.mkdir();
	}
    }

    class RandomAccessImpl implements IRandomAccess {
	private RandomAccessFile raf;

	public RandomAccessImpl(RandomAccessFile raf) {
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
