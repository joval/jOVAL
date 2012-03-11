// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.tree.INode;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.io.fs.CacheFilesystem;
import org.joval.io.fs.DefaultFile;
import org.joval.io.fs.FileAccessor;
import org.joval.io.fs.FileInfo;
import org.joval.os.windows.identity.LocalACE;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Defines extended attributes of a file on Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFile extends DefaultFile {
    public WindowsFile(CacheFilesystem fs, File file, String path) {
	super(fs, path);
	accessor = new WindowsAccessor(file);
	if (accessor.exists()) {
	    try {
		info = accessor.getInfo();
	    } catch (IOException e) {
		fs.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    @Override
    public FileAccessor getAccessor() {
	return accessor;
    }

    // Internal

    class WindowsAccessor extends DefaultAccessor implements IWindowsFileInfo {
	WindowsAccessor(File file) {
	    super(file);
	}

	@Override
	public FileInfo getInfo() throws IOException {
	    FileInfo fi = super.getInfo();
	    return new WindowsFileInfo(fi.ctime, fi.mtime, fi.atime, fi.type, fi.length, this);
	}

	// Implement IWindowsFileInfo

	public int getWindowsFileType() throws IOException {
	    try {
		int dirAttr = IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY & Kernel32Util.getFileAttributes(path);
		if (IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY == dirAttr) {
		    return IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY;
		} else {
		    return Kernel32Util.getFileType(getPath());
		}
	    } catch (Win32Exception e) {
		throw new IOException(e);
	    }
	}

	public IACE[] getSecurity() throws IOException {
	    try {
		WinNT.ACCESS_ACEStructure[] aces = Advapi32Util.getFileSecurity(getPath(), false);
		IACE[] acl = new IACE[aces.length];
		for (int i=0; i < aces.length; i++) {
		    acl[i] = new LocalACE(aces[i]);
		}
		return acl;
	    } catch (Win32Exception e) {
		throw new IOException(e);
	    }
	}
    }
}
