// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.io.fs.FileInfo;
import org.joval.util.StringTools;

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
    protected String canonicalPath;
    protected Properties extended;

    /**
     * Create an empty UnixFileInfo.
     */
    protected UnixFileInfo(){}

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
	this.canonicalPath = resolvePath(linkTarget);

	this.unixType = unixType;
	this.permissions = permissions;
	this.uid = uid;
	this.gid = gid;
	this.hasExtendedAcl = hasExtendedAcl;
	this.extended = extended; // extended data
    }

    public UnixFileInfo(FileInfo info, String path) {
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

    public UnixFileInfo(DataInput in) throws IOException {
	super(in);
	path = in.readUTF();
	String temp = in.readUTF();
	if ("".equals(temp)) {
	    canonicalPath = null;
	} else {
	    canonicalPath = temp;
	}
	unixType = in.readChar();
	permissions = in.readUTF();
	uid = in.readInt();
	gid = in.readInt();
	hasExtendedAcl = in.readBoolean();
	if (in.readBoolean()) {
	    extended = new Properties();
	    int pairs = in.readInt();
	    for (int i=0; i < pairs; i++) {
		extended.setProperty(in.readUTF(), in.readUTF());
	    }
	}
    }

    public void write(DataOutput out) throws IOException {
	super.write(out);
	out.writeUTF(path);
	if (canonicalPath == null) {
	    out.writeUTF("");
	} else {
	    out.writeUTF(canonicalPath);
	}
	out.writeChar(unixType);
	out.writeUTF(permissions);
	out.writeInt(uid);
	out.writeInt(gid);
	out.writeBoolean(hasExtendedAcl);
	if (extended == null) {
	    out.writeBoolean(false);
	} else {
	    out.writeBoolean(true);
	    Set<String> propertyNames = extended.stringPropertyNames();
	    out.writeInt(propertyNames.size());
	    for (String key : propertyNames) {
		out.writeUTF(key);
		out.writeUTF(extended.getProperty(key));
	    }
	}
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
	} else if (target.startsWith(IUnixFilesystem.DELIM_STR)) {
	    return target;
	} else {
	    Stack<String> stack = new Stack<String>();
	    for (String s : StringTools.toList(StringTools.tokenize(path, IUnixFilesystem.DELIM_STR))) {
		stack.push(s);
	    }
	    if (!stack.empty()) {
		stack.pop();
	    }
	    for (String next : StringTools.toList(StringTools.tokenize(target, IUnixFilesystem.DELIM_STR))) {
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
		StringBuffer elt = new StringBuffer(IUnixFilesystem.DELIM_STR);
		sb.insert(0, elt.append(stack.pop()).toString());
	    }
	    if (sb.length() == 0) {
		return IUnixFilesystem.DELIM_STR;
	    } else {
		return sb.toString();
	    }
	}
    }
}
