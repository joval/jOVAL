// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.SftpATTRS;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.unix.io.IUnixFile;
import org.joval.io.BaseFile;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * An IFile wrapper for an SFTP channel.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SftpFile extends BaseFile implements IUnixFile {
    private SftpFilesystem sfs;
    private SftpATTRS attrs = null;
    private String path, permissions;
    boolean tested=false, doesExist;

    SftpFile(SftpFilesystem fs, String path) {
	super(fs);
	sfs = fs;
	if (!path.equals(fs.getDelimiter()) && path.endsWith(fs.getDelimiter())) {
	    path = path.substring(0, path.lastIndexOf(fs.getDelimiter()));
	}
	this.path = path;
    }

    // Implement INode

    public String getCanonicalPath() {
	return toString();
    }

    // Implement IFile

    public long accessTime() throws IOException {
	if (exists()) {
	    return attrs.getAccessTime() * 1000L;
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    /**
     * Not really supported by this implementation.
     */
    public long createTime() throws IOException {
	return lastModified() * 1000L;
    }

    public boolean exists() throws IOException {
	if (!tested) {
	    try {
		attrs = sfs.getCS().lstat(path);
		permissions = attrs.getPermissionsString();
		if (permissions.length() != 10) {
		    throw new IOException("\"" + permissions + "\"");
		}
		doesExist = true;
		tested = true;
	    } catch (SftpException e) {
		switch(getErrorCode(e)) {
		  case SftpError.NO_SUCH_FILE:
		    doesExist = false;
		    tested = true;
		    break;

		  default:
		    throw new IOException(e);
		}
	    }
	}
	return doesExist;
    }

    public boolean mkdir() {
	try {
	    if (exists()) {
		return false;
	    } else {
		tested = false;
		sfs.getCS().mkdir(path + fs.getDelimiter());
		return exists();
	    }
	} catch (SftpException e) {
	    sfs.getLogger().warn(JOVALMsg.ERROR_IO, path, "mkdir");
	    sfs.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	} catch (IOException e) {
	    sfs.getLogger().warn(JOVALMsg.ERROR_IO, path, "mkdir");
	    sfs.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    public InputStream getInputStream() throws IOException {
	if (exists()) {
	    try {
		return sfs.getCS().get(path);
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	if (isLink()) {
	    return fs.getFile(toString()).getOutputStream(append);
	} else if (exists() && isDirectory()) {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_IO, path, JOVALSystem.getMessage(JOVALMsg.ERROR_IO_NOT_FILE));
	    throw new IOException(s);
	} else {
	    int mode = ChannelSftp.OVERWRITE;
	    if (append) {
		mode = ChannelSftp.APPEND;
	    }
	    try {
		return sfs.getCS().put(path, mode);
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	}
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isDirectory() throws IOException {
	if (isLink()) {
	    try {
		return new SftpFile(this).isDirectory();
	    } catch (FileNotFoundException e) {
		return false;
	    }
	} else if (exists()) {
	    return attrs.isDir();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean isFile() throws IOException {
	if (isLink()) {
	    return fs.getFile(toString()).isFile();
	} else if (exists()) {
	    return !isDirectory();
	} else {
	    return true;
	}
    }

    public boolean isLink() throws IOException {
	if (exists()) {
	    return attrs.isLink();
	} else {
	    return false;
	}
    }

    public long lastModified() throws IOException {
	if (exists()) {
	    return attrs.getModifiedTime() * 1000L;
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public long length() throws IOException {
	if (exists()) {
	    return attrs.getSize();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public String[] list() throws IOException {
	if (isLink()) {
	    return new SftpFile(this).list();
	} else {
	    try {
		List<ChannelSftp.LsEntry> list = sfs.getCS().ls(path);
		String[] children = new String[0];
		ArrayList<String> al = new ArrayList<String>();
		Iterator<ChannelSftp.LsEntry> iter = list.iterator();
		while (iter.hasNext()) {
		    ChannelSftp.LsEntry entry = iter.next();
		    if (!".".equals(entry.getFilename()) && !"..".equals(entry.getFilename())) {
			al.add(entry.getFilename());
		    }
		}
		return al.toArray(children);
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	}
    }

    public IFile[] listFiles() throws IOException {
	String[] names = list();
	IFile[] children = new IFile[names.length];
	for (int i=0; i < names.length; i++) {
	    children[i] = new SftpFile(sfs, getLocalName() + fs.getDelimiter() + names[i]);
	}
	return children;
    }

    public void delete() throws IOException {
	if (exists()) {
	    try {
		sfs.getCS().rm(path);
		tested = false;
	    } catch (SftpException e) {
		throw new IOException(e);
	    }
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public String getLocalName() {
	if (path.endsWith(fs.getDelimiter())) {
	    return path.substring(0, path.lastIndexOf(fs.getDelimiter()));
	} else {
	    return path;
	}
    }

    public String getName() {
	int ptr = path.lastIndexOf(fs.getDelimiter());
	if (ptr == -1) {
	    return path;
	} else {
	    return path.substring(ptr + fs.getDelimiter().length());
	}
    }

    public String toString() {
	try {
	    return sfs.getCS().realpath(path);
	} catch (SftpException e) {
	    return path;
	}
    }

    // Implement IUnixFile

    public String getUnixFileType() throws IOException {
	if (exists()) {
	    switch(permissions.charAt(0)) {
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
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public int getUserId() throws IOException {
	if (exists()) {
	    return attrs.getUId();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public int getGroupId() throws IOException {
	if (exists()) {
	    return attrs.getGId();
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean uRead() throws IOException {
	if (exists()) {
	    return permissions.charAt(1) == 'r';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean uWrite() throws IOException {
	if (exists()) {
	    return permissions.charAt(2) == 'w';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean uExec() throws IOException {
	if (exists()) {
	    return permissions.charAt(3) != '-';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean sUid() throws IOException {
	if (exists()) {
	    return permissions.charAt(3) == 's';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean gRead() throws IOException {
	if (exists()) {
	    return permissions.charAt(4) == 'r';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean gWrite() throws IOException {
	if (exists()) {
	    return permissions.charAt(5) == 'w';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean gExec() throws IOException {
	if (exists()) {
	    return permissions.charAt(6) != '-';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean sGid() throws IOException {
	if (exists()) {
	    return permissions.charAt(6) == 's';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean oRead() throws IOException {
	if (exists()) {
	    return permissions.charAt(7) == 'r';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean oWrite() throws IOException {
	if (exists()) {
	    return permissions.charAt(8) == 'w';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean oExec() throws IOException {
	if (exists()) {
	    return permissions.charAt(9) != '-';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean sticky() throws IOException {
	if (exists()) {
	    return permissions.charAt(9) == 't';
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    public boolean hasExtendedAcl() throws IOException {
	if (exists()) {
	    String[] exAttrs = attrs.getExtended();
	    return exAttrs != null && exAttrs.length > 0;
	} else {
	    throw new FileNotFoundException(path);
	}
    }

    // Private

    /**
     * Create an SftpFile that steps into a link.
     */
    private SftpFile(SftpFile link) {
	super(link.sfs);
	sfs = link.sfs;
	this.path = link.path + fs.getDelimiter();
    }

    private int getErrorCode(SftpException e) {
	try {
	    String s = e.toString();
	    return Integer.parseInt(s.substring(0, s.indexOf(":")));
	} catch (Exception ex) {
	    return -1;
	}
    }
}
