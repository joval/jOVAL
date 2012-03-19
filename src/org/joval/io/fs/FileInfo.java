// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;

/**
 * A FileInfo object contains information about a file. It can be constructed from a FileAccessor, or directly from
 * data gathered through other means (i.e., cached data).  Subclasses are used to store platform-specific file
 * information.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileInfo implements IFileEx {
    public enum Type {FILE, DIRECTORY, LINK;}

    protected long ctime=IFile.UNKNOWN_TIME, mtime=IFile.UNKNOWN_TIME, atime=IFile.UNKNOWN_TIME, length=-1L;
    protected Type type = null;

    public FileInfo() {}

    public FileInfo(FileAccessor access, Type type) throws IOException {
	this.type = type;
	ctime = access.getCtime();
	mtime = access.getMtime();
	atime = access.getAtime();
	length = access.getLength();
    }

    public FileInfo(long ctime, long mtime, long atime, Type type, long length) {
	this.ctime = ctime;
	this.mtime = mtime;
	this.atime = atime;
	this.type = type;
	this.length = length;
    }

    public long getCtime() {
	return ctime;
    }

    public long getMtime() {
	return mtime;
    }

    public long getAtime() {
	return atime;
    }

    public long getLength() {
	return length;
    }

    public Type getType() {
	return type;
    }
}
