// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

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
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.io.fs.CacheFile;
import org.joval.io.fs.FileAccessor;
import org.joval.io.fs.FileInfo;
import org.joval.os.windows.io.WindowsFileInfo;
import org.joval.os.windows.remote.identity.SmbACE;
import org.joval.util.JOVALMsg;

/**
 * An IFile wrapper for an SmbFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SmbAccessor extends FileAccessor implements IWindowsFileInfo {
    private IFilesystem fs;
    private SmbFile smbFile;

    SmbAccessor(IFilesystem fs, SmbFile smbFile) {
	this.fs = fs;
	this.smbFile = smbFile;
    }

    public String toString() {
	return smbFile.getUncPath();
    }

    // Implement IFile

    public long getCtime() throws IOException {
	return smbFile.createTime();
    }

    public long getMtime() throws IOException {
	return smbFile.lastModified();
    }

    public long getAtime() throws IOException {
	return IFile.UNKNOWN_TIME;
    }

    public boolean exists() {
	try {
	    return smbFile.exists();
	} catch (IOException e) {
	    fs.getLogger().warn(JOVALMsg.ERROR_IO, toString(), e.getMessage());
	    fs.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return false;
    }

    public boolean mkdir() {
	try {
	    smbFile.mkdir();
	    return true;
	} catch (SmbException e) {
	    fs.getLogger().warn(JOVALMsg.ERROR_IO, toString());
	    fs.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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

    public long getLength() throws IOException {
	return smbFile.length();
    }

    public void delete() throws IOException {
	smbFile.delete();
    }

    public String getCanonicalPath() {
	String uncCP = smbFile.getCanonicalPath();
	return uncCP.substring(6).replaceAll("\\/","\\\\");
    }

    public FileInfo getInfo() throws IOException {
	return new SmbFileInfo(this, isDirectory() ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);
    }

    public String[] list() throws IOException {
	if (smbFile.isDirectory()) {
	    return smbFile.list();
	} else {
	    return null;
	}
    }

    // Implement IWindowsFileInfo

    public int getWindowsFileType() throws IOException {
	if (isDirectory()) {
	    return IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY;
	} else {
	    switch(smbFile.getType()) {
	      case SmbFile.TYPE_FILESYSTEM:
		return IWindowsFileInfo.FILE_TYPE_DISK;
    
	      case SmbFile.TYPE_WORKGROUP:
	      case SmbFile.TYPE_SERVER:
	      case SmbFile.TYPE_SHARE:
		return IWindowsFileInfo.FILE_TYPE_REMOTE;
    
	      case SmbFile.TYPE_NAMED_PIPE:
		return IWindowsFileInfo.FILE_TYPE_PIPE;
    
	      case SmbFile.TYPE_PRINTER:
	      case SmbFile.TYPE_COMM:
		return IWindowsFileInfo.FILE_TYPE_CHAR;

	      default:
		return IWindowsFileInfo.FILE_TYPE_UNKNOWN;
	    }
	}
    }

    public IACE[] getSecurity() throws IOException {
	ACE[] aa = smbFile.getSecurity();
	IACE[] acl = new IACE[aa.length];
	for (int i=0; i < aa.length; i++) {
	    acl[i] = new SmbACE(aa[i]);
	}
	return acl;
    }
}
