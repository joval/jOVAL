// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private IFile f;
    private boolean hasExtendedAcl = false;
    private String permissions = null, path = null, canonicalPath = null;
    private int uid, gid;
    private long size = -1L;
    private char unixType = NULL;
    private Date lastModified = null;

    public UnixFile(IUnixSession session, IFile f) throws IOException {
	this.f = f;
	try {
	    String command = null;
	    switch(session.getFlavor()) {
	      case MACOSX: {
		command = "/bin/ls -ldn " + f.getLocalName();
		break;
	      }
    
	      case AIX:
	      case SOLARIS: {
		command = "/usr/bin/ls dn " + f.getLocalName();
		break;
	      }
    
	      case LINUX: {
		command = "/bin/ls -dn " + f.getLocalName();
		break;
	      }

	      default:
		throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, session.getFlavor()));
	    }

	    if (f.exists()) {
		load(SafeCLI.exec(command, session, IUnixSession.Timeout.S));
	    }
	} catch (Exception e) {
	    throw new IOException(e);
	}
    }

    // Implement INode

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	return getChildren(null);
    }

    public Collection<INode> getChildren(Pattern p) throws NoSuchElementException, UnsupportedOperationException {
	if (ufs == null) {
	    return f.getChildren(p);
	} else {
	    try {
		if (isLink()) {
		    return ufs.getFile(getCanonicalPath()).getChildren();
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
    }

    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException {
	if (ufs == null) {
	    return f.getChild(name);
	} else {
	    try {
		return ufs.getFile(getPath() + "/" + name);
	    } catch (IOException e) {
		throw new UnsupportedOperationException(e);
	    }
	}
    }

    public String getName() {
	return f.getName();
    }

    public String getPath() {
	if (f == null) {
	    return path;
	} else {
	    return f.getPath();
	}
    }

    public String getCanonicalPath() {
	if (unixType == NULL) {
	    return f.getCanonicalPath();
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
	return f.getType();
    }

    public boolean hasChildren() throws NoSuchElementException {
	if (unixType == NULL) {
	    return f.hasChildren();
	} else {
	    switch(unixType) {
	      case DIR_TYPE:
		try {
		    return getChildren().size() > 0;
		} catch (UnsupportedOperationException e) {
		}
		return false;

	      case LINK_TYPE:
		if (ufs == null) {
		    return f.hasChildren();
		} else {
		    try {
			return ufs.getFile(canonicalPath).hasChildren();
		    } catch (IOException e) {
			throw new UnsupportedOperationException(e);
		    }
		}

	      default:
		return false;
	    }
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
	    return f.exists();
	} else {
	    return true;
	}
    }

    public boolean mkdir() {
	return f.mkdir();
    }

    public InputStream getInputStream() throws IOException {
	return f.getInputStream();
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	return f.getOutputStream(append);
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return f.getRandomAccess(mode);
    }

    public boolean isDirectory() throws IOException {
	if (unixType == NULL) {
	    return f.isDirectory();
	} else {
	    return unixType == DIR_TYPE;
	}
    }

    public boolean isFile() throws IOException {
	if (unixType == NULL) {
	    return f.isFile();
	} else {
	    return unixType == FILE_TYPE;
	}
    }

    public boolean isLink() throws IOException {
	if (permissions == null) {
	    return f.isLink();
	} else {
	    return unixType == LINK_TYPE;
	}
    }

    public long lastModified() throws IOException {
	if (lastModified == null) {
	    return f.lastModified();
	} else {
	    return lastModified.getTime();
	}
    }

    public long length() throws IOException {
	if (size == -1) {
	    return f.length();
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
	for (INode node : getChildren()) {
	    list.add((IFile)node);
	}
	return list.toArray(new IFile[list.size()]);
    }

    public void delete() throws IOException {
	f.delete();
	unixType = NULL;
    }

    public String getLocalName() {
	return f.getLocalName();
    }

    public String toString() {
	return f.toString();
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

    // Internal

    /**
     * All files loaded from cache are assumed to exist.
     */
    UnixFile(String line) {
	load(line);
    }

    void set(UnixFilesystem ufs, IFile f) {
	this.ufs = ufs;
	this.f = f;
    }

    /**
     * Non-existent files should never have information loaded.
     */
    private void load(String line) {
	unixType = line.charAt(0);
	permissions = line.substring(1, 10);
	if (line.charAt(10) == '+') {
	    hasExtendedAcl = true;
	}
	StringTokenizer tok = new StringTokenizer(line.substring(11));
	String linkCount = tok.nextToken();
	uid = Integer.parseInt(tok.nextToken());
	gid = Integer.parseInt(tok.nextToken());
	switch(unixType) {
	  case CHAR_TYPE:
	  case BLOCK_TYPE:
	    tok.nextToken();
	  default:
 	   break;
	}
	try {
	    size = Long.parseLong(tok.nextToken());
	} catch (NumberFormatException e) {
	}

	String dateStr = tok.nextToken("/").trim();
	try {
	    if (dateStr.indexOf(":") == -1) {
		lastModified = new SimpleDateFormat("MMM dd  yyyy").parse(dateStr);
	    } else {
		lastModified = new SimpleDateFormat("MMM dd HH:mm").parse(dateStr);
	    }
	} catch (ParseException e) {
	    e.printStackTrace();
	}
	
	int begin = line.indexOf("/");
	if (begin > 0) {
	    int end = line.indexOf("->");
	    if (end == -1) {
	        path = line.substring(begin).trim();
	    } else if (end > begin) {
	        path = line.substring(begin, end).trim();
		canonicalPath = line.substring(end+2).trim();
	    }
	}
    }
}
