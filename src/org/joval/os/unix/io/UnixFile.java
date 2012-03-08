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

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.unix.io.IUnixFile;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.io.BaseFile;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Gets extended attributes of a file on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFile extends BaseFile implements IUnixFile {
    public static final char NULL	= '0';
    public static final char DIR_TYPE	= 'd';
    public static final char FIFO_TYPE	= 'p';
    public static final char LINK_TYPE	= 'l';
    public static final char BLOCK_TYPE	= 'b';
    public static final char CHAR_TYPE	= 'c';
    public static final char SOCK_TYPE	= 's';
    public static final char FILE_TYPE	= '-';

    private UnixFilesystem ufs;
    private IFile accessor;

    boolean hasExtendedAcl = false;
    String permissions = null, canonicalPath = null, linkPath = null;
    int uid, gid;
    long size = -1L;
    char unixType = NULL;
    Date lastModified = null;

    /**
     * Create a UnixFile with a live IFile accessor.
     */
    UnixFile(UnixFilesystem ufs, IFile accessor) {
	super(ufs, accessor.getPath());
	this.ufs = ufs;
	this.accessor = accessor;
	accessible = true;
    }

    /**
     * Create a UnixFile whose information will be populated externally from the cache.
     */
    UnixFile(UnixFilesystem ufs) {
	super(ufs, "PLACEHOLDER");
	this.ufs = ufs;
    }

    void setPath(String path) {
	this.path = path;
    }

    // Implement ICacheable

    public boolean isLink() {
	if (permissions == null) {
	    try {
		return getAccessor().isLink();
	    } catch (IOException e) {
	    }
	    return false;
	} else {
	    return unixType == LINK_TYPE;
	}
    }

    @Override
    public String getLinkPath() throws IllegalStateException {
	if (isLink()) {
	    if (linkPath == null) {
		try {
		    linkPath = getAccessor().getCanonicalPath();
		} catch (IOException e) {
		}
	    }
	    return linkPath;
	} else {
	    return super.getLinkPath(); // throw exception
	}
    }

    @Override
    public void setCachePath(String cachePath) {
	canonicalPath = cachePath;
	linkPath = cachePath;
    }

    // Implement IFile

    public long accessTime() throws IOException {
	return lastModified();
    }

    public long createTime() throws IOException {
	return lastModified();
    }

    public boolean exists() throws IOException {
	if (unixType == NULL) {
	    return getAccessor().exists();
	} else {
	    return true;
	}
    }

    public boolean mkdir() {
	try {
	    return getAccessor().mkdir();
	} catch (IOException e) {
	    return false;
	}
    }

    public InputStream getInputStream() throws IOException {
	return getAccessor().getInputStream();
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	return getAccessor().getOutputStream(append);
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return getAccessor().getRandomAccess(mode);
    }

    public boolean isDirectory() throws IOException {
	if (unixType == NULL) {
	    return getAccessor().isDirectory();
	} else {
	    switch(unixType) {
	      case DIR_TYPE:
		return true;

	      case LINK_TYPE:
		return ufs.getFile(getCanonicalPath()).isDirectory();

	      default:
		return false;
	    }
	}
    }

    public boolean isFile() throws IOException {
	if (unixType == NULL) {
	    return getAccessor().isFile();
	} else {
	    return unixType == FILE_TYPE;
	}
    }

    public long lastModified() throws IOException {
	if (lastModified == null) {
	    return getAccessor().lastModified();
	} else {
	    return lastModified.getTime();
	}
    }

    public long length() throws IOException {
	if (size == -1) {
	    return getAccessor().length();
	} else {
	    return size;
	}
    }

    public String[] list() throws IOException {
	return getAccessor().list();
    }

    public void delete() throws IOException {
	getAccessor().delete();
	unixType = NULL;
    }

    public String getCanonicalPath() {
	if (unixType == NULL) {
	    try {
		return getAccessor().getCanonicalPath();
	    } catch (IOException e) {
		return path;
	    }
	} else {
	    switch(unixType) {
	      case LINK_TYPE:
		return linkPath;

	      default:
		return getPath();
	    }
	}
    }

    public String toString() {
	try {
	    return getAccessor().toString();
	} catch (IOException e) {
	    return getPath();
	}
    }

    // Implement IUnixFile

    public String getUnixFileType() {
	switch(unixType) {
	  case DIR_TYPE:
	    return "directory";
	  case FIFO_TYPE:
	    return "fifo";
	  case LINK_TYPE:
	    return "symlink";
	  case BLOCK_TYPE:
	    return "block";
	  case CHAR_TYPE:
	    return "character";
	  case SOCK_TYPE:
	    return "socket";
	  case FILE_TYPE:
	  default:
	    return "regular";
	}
    }

    public int getUserId() {
	return uid;
    }

    public int getGroupId() {
	return gid;
    }

    public boolean uRead() {
	return permissions.charAt(0) == 'r';
    }

    public boolean uWrite() {
	return permissions.charAt(1) == 'w';
    }

    public boolean uExec() {
	return permissions.charAt(2) != '-';
    }

    public boolean sUid() {
	return permissions.charAt(2) == 's';
    }

    public boolean gRead() {
	return permissions.charAt(3) == 'r';
    }

    public boolean gWrite() {
	return permissions.charAt(4) == 'w';
    }

    public boolean gExec() {
	return permissions.charAt(5) != '-';
    }

    public boolean sGid() {
	return permissions.charAt(5) == 's';
    }

    public boolean oRead() {
	return permissions.charAt(6) == 'r';
    }

    public boolean oWrite() {
	return permissions.charAt(7) == 'w';
    }

    public boolean oExec() {
	return permissions.charAt(8) != '-';
    }

    public boolean sticky() {
	return permissions.charAt(8) == 't';
    }

    public boolean hasExtendedAcl() {
	return hasExtendedAcl;
    }

    // Private

    private IFile getAccessor() throws IOException {
	if (accessor == null) {
	    accessor = ufs.getFile(path, IFile.NOCACHE);
	    accessible = true;
	}
	return accessor;
    }
}
