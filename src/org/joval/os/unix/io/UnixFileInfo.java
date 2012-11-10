// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;

import org.joval.io.AbstractFilesystem;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.util.StringTools;

/**
 * Implements extended attributes of a file on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFileInfo extends AbstractFilesystem.FileInfo implements IUnixFileInfo {
    static final String DELIM = "/";

    private String path;
    private String linkTarget;
    private String canonicalPath;
    private boolean hasExtendedAcl = false;
    private String permissions = null;
    private int uid, gid;
    private char unixType = FILE_TYPE;
    private Properties extended;

    public UnixFileInfo(long ctime, long mtime, long atime, Type type, long length, String path, String linkTarget,
			char unixType, String permissions, int uid, int gid, boolean hasExtendedAcl) {
	this(ctime, mtime, atime, type, length, path, linkTarget, unixType, permissions, uid, gid, hasExtendedAcl, null);
    }

    /**
     * Create a UnixFile with a live IFile accessor.
     */
    public UnixFileInfo(long ctime, long mtime, long atime, Type type, long length, String path, String linkTarget,
			char unixType, String permissions, int uid, int gid, boolean hasExtendedAcl, Properties extended) {

	super(ctime, mtime, atime, type, length);

	this.path = path;
	this.linkTarget = linkTarget;
	this.canonicalPath = resolvePath(linkTarget);

	this.unixType = unixType;
	this.permissions = permissions;
	this.uid = uid;
	this.gid = gid;
	this.hasExtendedAcl = hasExtendedAcl;
	this.extended = extended; // extended data
    }

    UnixFileInfo(AbstractFilesystem.FileInfo info, String path) {
	ctime = info.getCtime();
	mtime = info.getMtime();
	atime = info.getAtime();
	type = info.getType();
	length = info.getLength();
	this.path = path;
	permissions = "-rwxrwxrwx";
	uid = -1;
	gid = -1;
    }

    public String getPath() {
	return path;
    }

    public String getLinkPath() {
	return linkTarget;
    }

    public String getCanonicalPath() {
	return canonicalPath;
    }

    // Implement IUnixFileInfo

    public String getUnixFileType() {
	switch(unixType) {
	  case DIR_TYPE:
	    return FILE_TYPE_DIR;
	  case FIFO_TYPE:
	    return FILE_TYPE_FIFO;
	  case LINK_TYPE:
	    return FILE_TYPE_LINK;
	  case BLOCK_TYPE:
	    return FILE_TYPE_BLOCK;
	  case CHAR_TYPE:
	    return FILE_TYPE_CHAR;
	  case SOCK_TYPE:
	    return FILE_TYPE_SOCK;
	  case FILE_TYPE:
	  default:
	    return FILE_TYPE_REGULAR;
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

    public String getExtendedData(String key) throws NoSuchElementException {
	if (extended != null && extended.containsKey(key)) {
	    return extended.getProperty(key);
	} else {
	    throw new NoSuchElementException(key);
	}
    }

    // Private

    /**
     * Resolve an absolute path from a relative path from a base file path.
     *
     * @arg target the link target, which might be a path relative to the origin.
     *
     * @returns an absolute path to the target
     */
    private String resolvePath(String target) throws UnsupportedOperationException {
	if (target == null) {
	    return null;
	} else if (target.startsWith(DELIM)) {
	    return target;
	} else {
	    Stack<String> stack = new Stack<String>();
	    for (String s : StringTools.toList(StringTools.tokenize(path, DELIM))) {
		stack.push(s);
	    }
	    if (!stack.empty()) {
		stack.pop();
	    }
	    for (String next : StringTools.toList(StringTools.tokenize(target, DELIM))) {
		if (next.equals(".")) {
		    // stay in the same place
		} else if (next.equals("..")) {
		    if (stack.empty()) {
			// links above root stay at root
		    } else {
			stack.pop();
		    }
		} else {
		    stack.push(next);
		}
	    }
	    StringBuffer sb = new StringBuffer();
	    while(!stack.empty()) {
		StringBuffer elt = new StringBuffer(DELIM);
		sb.insert(0, elt.append(stack.pop()).toString());
	    }
	    if (sb.length() == 0) {
		return DELIM;
	    } else {
		return sb.toString();
	    }
	}
    }
}
