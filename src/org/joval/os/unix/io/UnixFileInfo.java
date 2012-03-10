// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.io.fs.FileInfo;

/**
 * Implements extended attributes of a file on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFileInfo extends FileInfo implements IUnixFileInfo {
    protected boolean hasExtendedAcl = false;
    protected String permissions = null;
    protected int uid, gid;
    protected char unixType = FILE_TYPE;
    protected String path;

    protected UnixFileInfo(){}

    /**
     * Create a UnixFile with a live IFile accessor.
     */
    UnixFileInfo (long ctime, long mtime, long atime, Type type, long length, String linkTarget,
		  char unixType, String permissions, int uid, int gid, boolean hasExtendedAcl, String path) {

	super(ctime, mtime, atime, type, length, linkTarget);
	this.unixType = unixType;
	this.permissions = permissions;
	this.uid = uid;
	this.gid = gid;
	this.hasExtendedAcl = hasExtendedAcl;
	this.path = path;
    }

    // Implement IUnixFileInfo

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
}
