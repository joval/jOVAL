// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.io.IFileEx;
import org.joval.intf.util.tree.INode;
import org.joval.util.CachingHierarchy;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

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

    public static final long UNKNOWN_TIME = -1L;

    public long ctime, mtime, atime, length;
    public Type type;
    public String canonicalPath;

    public FileInfo() {}

    public FileInfo(FileAccessor access, Type type) throws IOException {
	this.type = type;
	ctime = access.getCtime();
	mtime = access.getMtime();
	atime = access.getAtime();
	length = access.getLength();
	canonicalPath = access.getCanonicalPath();
    }

    public FileInfo(long ctime, long mtime, long atime, Type type, long length) {
	this(ctime, mtime, atime, type, length, null);
    }

    public FileInfo(long ctime, long mtime, long atime, Type type, long length, String linkTarget) {
	this.ctime = ctime;
	this.mtime = mtime;
	this.atime = atime;
	this.type = type;
	this.length = length;
	this.canonicalPath = linkTarget;
    }
}
