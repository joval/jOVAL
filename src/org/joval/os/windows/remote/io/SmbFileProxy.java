// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.List;

import jcifs.smb.ACE;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.smb.SmbRandomAccessFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFile;
import org.joval.io.BaseFile;
import org.joval.os.windows.remote.identity.SmbACE;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * An IFile wrapper for an SmbFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SmbFileProxy extends BaseFile implements IWindowsFile {
    private SmbFile smbFile;
    private String localName;
    private IACE[] aces = null;

    SmbFileProxy(IFilesystem fs, SmbFile smbFile, String localName) {
	super(fs);
	this.smbFile = smbFile;
	if (localName.endsWith(fs.getDelimiter())) {
	    this.localName = localName.substring(0, localName.lastIndexOf(fs.getDelimiter()));
	} else {
	    this.localName = localName;
	}
    }

    SmbFile getSmbFile() {
	return smbFile;
    }

    // Implement INode

    public String getCanonicalPath() {
	String uncCP = smbFile.getCanonicalPath();
	return uncCP.substring(6).replaceAll("\\/","\\\\");
    }

    // Implement IFile

    /**
     * Not really supported by this implementation.
     */
    public long accessTime() throws IOException {
	return lastModified();
    }

    public long createTime() throws IOException {
	return smbFile.createTime();
    }

    public boolean exists() throws IOException {
	return smbFile.exists();
    }

    public boolean mkdir() {
	try {
	    smbFile.mkdir();
	    return true;
	} catch (SmbException e) {
	    fs.getLogger().warn(JOVALMsg.ERROR_IO, toString());
	    fs.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    public InputStream getInputStream() throws IOException {
	return new SmbFileInputStream(smbFile);
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
	return new SmbFileOutputStream(smbFile, append);
    }

    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return new SmbRandomAccessProxy(new SmbRandomAccessFile(smbFile, mode));
    }

    public boolean isDirectory() throws IOException {
	return smbFile.isDirectory();
    }

    public boolean isFile() throws IOException {
	return smbFile.isFile();
    }

    public boolean isLink() throws IOException {
	return smbFile.getDfsPath() != null;
    }

    public long lastModified() throws IOException {
	return smbFile.lastModified();
    }

    public long length() throws IOException {
	return smbFile.length();
    }

    public String[] list() throws IOException {
	return smbFile.list();
    }

    public IFile[] listFiles() throws IOException {
	SmbFile[] files = smbFile.listFiles();
	IFile[] children = new IFile[files.length];
	for (int i=0; i < files.length; i++) {
	    StringBuffer sb = new StringBuffer(localName);
	    sb.append(fs.getDelimiter());
	    sb.append(getName(files[i].getName()));
	    if (files[i].isDirectory()) {
		sb.append(fs.getDelimiter());
	    }
	    children[i] = fs.getFile(sb.toString());
	}
	return children;
    }

    public void delete() throws IOException {
	smbFile.delete();
    }

    public String getLocalName() {
	return localName;
    }

    public String getName() {
	return getName(smbFile.getName());
    }

    public String toString() {
	return smbFile.getUncPath();
    }

    // Implement IWindowsFile

    public int getWindowsFileType() throws IOException {
	if (isDirectory()) {
	    return FILE_ATTRIBUTE_DIRECTORY;
	} else {
	    switch(smbFile.getType()) {
	      case SmbFile.TYPE_FILESYSTEM:
		return FILE_TYPE_DISK;
    
	      case SmbFile.TYPE_WORKGROUP:
	      case SmbFile.TYPE_SERVER:
	      case SmbFile.TYPE_SHARE:
		return FILE_TYPE_REMOTE;
    
	      case SmbFile.TYPE_NAMED_PIPE:
		return FILE_TYPE_PIPE;
    
	      case SmbFile.TYPE_PRINTER:
	      case SmbFile.TYPE_COMM:
		return FILE_TYPE_CHAR;
    
	      default:
		return FILE_TYPE_UNKNOWN;
	    }
	}
    }

    public IACE[] getSecurity() throws IOException {
	if (aces == null) {
	    ACE[] aa = smbFile.getSecurity();
	    aces = new IACE[aa.length];
	    for (int i=0; i < aa.length; i++) {
		aces[i] = new SmbACE(aa[i]);
	    }
	}
	return aces;
    }

    // Private

    private String getName(String name) {
	if (name.endsWith(SmbFilesystem.SMBURL_DELIM_STR)) {
	    return name.substring(0, name.lastIndexOf(SmbFilesystem.SMBURL_DELIM_STR));
	} else {
	    return name;
	}
    }
}
