// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.joval.intf.io.IFile;
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
    private IFile f;
    private boolean hasExtendedAcl = false;
    private String permissions;
    private int uid, gid;
    private char unixType;

    public UnixFile(IUnixSession session, IFile f) throws IOException {
	this.f = f;
	try {
	    String command = null;
	    switch(session.getFlavor()) {
	      case MACOSX: {
		command = "/bin/ls -ldn " + f.getLocalName();
		break;
	      }
    
	      case SOLARIS: {
		command = "/usr/bin/ls -n " + f.getLocalName();
		break;
	      }
    
	      case LINUX: {
		command = "/bin/ls -dn " + f.getLocalName();
		break;
	      }

	      default:
		throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, session.getFlavor()));
	    }
    
	    String line = SafeCLI.exec(command, session, IUnixSession.TIMEOUT_S);
	    unixType = line.charAt(0);
	    permissions = line.substring(1, 10);
	    if (line.charAt(11) == '+') {
		hasExtendedAcl = true;
	    }
	    StringTokenizer tok = new StringTokenizer(line.substring(12));
	    uid = Integer.parseInt(tok.nextToken());
	    gid = Integer.parseInt(tok.nextToken());
	} catch (Exception e) {
	    throw new IOException(e);
	}
    }

    // Implement INode

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	return f.getChildren();
    }

    public Collection<INode> getChildren(Pattern p) throws NoSuchElementException, UnsupportedOperationException {
	return f.getChildren(p);
    }

    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException {
	return f.getChild(name);
    }

    public String getName() {
	return f.getName();
    }

    public String getPath() {
	return f.getPath();
    }

    public String getCanonicalPath() {
	return f.getCanonicalPath();
    }

    public Type getType() {
	return f.getType();
    }

    public boolean hasChildren() throws NoSuchElementException {
	return f.hasChildren();
    }

    // Implement IFile

    public long accessTime() throws IOException {
	return f.accessTime();
    }

    public long createTime() throws IOException {
	return f.createTime();
    }

    public boolean exists() throws IOException {
	return f.exists();
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
	return f.isDirectory();
    }

    public boolean isFile() throws IOException {
	return f.isFile();
    }

    public boolean isLink() throws IOException {
	return f.isLink();
    }

    public long lastModified() throws IOException {
	return f.lastModified();
    }

    public long length() throws IOException {
	return f.length();
    }

    public String[] list() throws IOException {
	return f.list();
    }

    public IFile[] listFiles() throws IOException {
	ArrayList<IFile> list = new ArrayList<IFile>();
	for (INode node : f.getChildren()) {
	    list.add((IFile)node);
	}
	return list.toArray(new IFile[list.size()]);
    }

    public void delete() throws IOException {
	f.delete();
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
	  case 'd':
	    return "directory";
	  case 'p':
	    return "fifo";
	  case 'l':
	    return "symlink";
	  case 'b':
	    return "block";
	  case 'c':
	    return "character";
	  case 's':
	    return "socket";
	  case '-':
	  default:
	    return "file";
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
