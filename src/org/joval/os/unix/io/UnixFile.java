// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFileEx;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.io.fs.CacheFile;
import org.joval.io.fs.DefaultFile;
import org.joval.io.fs.FileAccessor;
import org.joval.io.fs.FileInfo;
import org.joval.util.SafeCLI;

/**
 * Implements an IFile with info having extended Unix attributes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFile extends DefaultFile {
    private UnixFilesystem ufs;

    /**
     * Create a UnixFile using a live accessor.
     */
    UnixFile(UnixFilesystem ufs, CacheFile cf, String path) throws IOException {
	super(ufs, path);
	this.ufs = ufs;
	this.accessor = new UnixFileAccessor(cf.getAccessor());
    }

    /**
     * Create a UnixFile using information.
     */
    UnixFile(UnixFilesystem ufs, UnixFileInfo info, String path) {
	super(ufs, path);
	this.ufs = ufs;
	this.info = info;
    }

    @Override
    public String toString() {
	return getPath();
    }

    @Override
    public FileAccessor getAccessor() {
	if (accessor == null) {
	    try {
		CacheFile cf = (CacheFile)ufs.accessResource(getPath(), ufs.getDefaultFlags());
		accessor = new UnixFileAccessor(cf.getAccessor());
	    } catch (IOException e) {
		throw new RuntimeException(e);
	    }
	}
	return accessor;
    }

    @Override
    public String getCanonicalPath() throws IOException {
	if (info == null) {
	    return getAccessor().getCanonicalPath();
	} else {
	    return ((UnixFileInfo)info).canonicalPath;
	}
    }

    // Private

    /**
     * A proxy for a FileAccessor, that leverages the UnixFilesystem to get UnixFileInformation.
     */
    private class UnixFileAccessor extends FileAccessor {
	private FileAccessor internal;

	UnixFileAccessor(FileAccessor internal) {
	    this.internal = internal;
	}

	public FileInfo getInfo() throws IOException {
	    try {
		info = ufs.getUnixFileInfo(getPath());
		if (info == null) {
		    IUnixFilesystemDriver driver = ufs.getDriver();
		    String cmd = driver.getStatCommand() + " " + getPath();
		    info = (FileInfo)driver.nextFileInfo(SafeCLI.multiLine(cmd, ufs.us, IUnixSession.Timeout.S).iterator());
		}
		return info;
	    } catch (Exception e) {
		if (e instanceof IOException) {
		    throw (IOException)e;
		} else {
		    throw new IOException(e);
		}
	    }
	}

	public boolean exists() {
	    if (info == null) {
		return internal.exists();
	    } else {
		return true;
	    }
	}

	public long getCtime() throws IOException {
	    return internal.getCtime();
	}

	public long getMtime() throws IOException {
	    return internal.getMtime();
	}

	public long getAtime() throws IOException {
	    return internal.getAtime();
	}

	public long getLength() throws IOException {
	    return internal.getLength();
	}

	public IRandomAccess getRandomAccess(String mode) throws IOException {
	    return internal.getRandomAccess(mode);
	}

	public InputStream getInputStream() throws IOException {
	    return internal.getInputStream();
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
	    return internal.getOutputStream(append);
	}

	public String getCanonicalPath() throws IOException {
	    return internal.getCanonicalPath();
	}

	public String[] list() throws IOException {
	    return internal.list();
	}

	public boolean mkdir() {
	    return internal.mkdir();
	}

	public void delete() throws IOException {
	    info = null;
	    internal.delete();
	}
    }
}
