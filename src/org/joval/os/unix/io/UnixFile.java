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
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.unix.io.IUnixFile;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Gets extended attributes of a file on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFile implements IUnixFile {
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
    String permissions = null, path = null, canonicalPath = null;
    int uid, gid;
    long size = -1L;
    char unixType = NULL;
    Date lastModified = null;

    /**
     * Create a UnixFile with a live IFile accessor.
     */
    UnixFile(UnixFilesystem ufs, IFile accessor) {
	this.ufs = ufs;
	this.accessor = accessor;
    }

    /**
     * Create a UnixFile whose information will be populated externally from the cache.
     */
    UnixFile(UnixFilesystem ufs) {
	this.ufs = ufs;
    }

    // Implement INode

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	return getChildren(null);
    }

    public Collection<INode> getChildren(Pattern p) throws NoSuchElementException, UnsupportedOperationException {
	try {
	    if (isLink()) {
		return ((IFile)ufs.lookup(getCanonicalPath())).getChildren(p);
	    } else if (isDirectory()) {
		Collection<INode> children = new Vector<INode>();
		try {
		    String[] sa = ufs.list(this);
		    for (int i=0; i < sa.length; i++) {
			if (p == null || p.matcher(sa[i]).find()) {
			    children.add(getChild(sa[i]));
			}
		    }
		} catch (UnsupportedOperationException e) {
		}
		return children;
	    } else {
		throw new UnsupportedOperationException(getPath());
	    }
	} catch (IOException e) {
	    throw new UnsupportedOperationException(e);
	}
    }

    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException {
	String prefix = getPath();
	StringBuffer childPath = new StringBuffer(prefix);
	if (!prefix.endsWith(UnixFilesystem.DELIM_STR)) {
	    childPath.append(UnixFilesystem.DELIM_STR);
	}
	childPath.append(name);
	return (IFile)ufs.lookup(childPath.toString());
    }

    public String getName() {
	return path.substring(path.lastIndexOf("/")+1);
    }

    public String getPath() {
	return path;
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
		return canonicalPath;

	      default:
		return getPath();
	    }
	}
    }

    public Type getType() {
	try {
	    return getAccessor().getType();
	} catch (IOException e) {
	    return INode.Type.LEAF;
	}
    }

    public boolean hasChildren() throws NoSuchElementException {
	try {
	    if (unixType == NULL) {
		return getAccessor().hasChildren();
	    } else {
		switch(unixType) {
		  case DIR_TYPE:
		    try {
			return getChildren().size() > 0;
		    } catch (UnsupportedOperationException e) {
		    }
		    return false;
    
		  case LINK_TYPE:
		    return ufs.lookup(canonicalPath).hasChildren();
    
		  default:
		    return false;
		}
	    }
	} catch (IOException e) {
	    ufs.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new NoSuchElementException(e.getMessage());
	}
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
		return ((IFile)ufs.lookup(getCanonicalPath())).isDirectory();

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

    public boolean isLink() throws IOException {
	if (permissions == null) {
	    return getAccessor().isLink();
	} else {
	    return unixType == LINK_TYPE;
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
	IFile[] fa = listFiles();
	String[] sa = new String[fa.length];
	for (int i=0; i < sa.length; i++) {
	    sa[i] = fa[i].getName();
	}
	return sa;
    }

    public IFile[] listFiles() throws IOException {
	Vector<IFile> list = new Vector<IFile>();
	try {
	    for (INode node : getChildren()) {
		list.add((IFile)node);
	    }
	    return list.toArray(new IFile[list.size()]);
	} catch (UnsupportedOperationException e) {
	    throw new IOException(e);
	} catch (NoSuchElementException e) {
	    throw new IOException(e);
	}
    }

    public void delete() throws IOException {
	getAccessor().delete();
	unixType = NULL;
    }

    public String getLocalName() {
	return path;
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
	    accessor = ufs.getFileImpl(path);
	}
	return accessor;
    }
}
