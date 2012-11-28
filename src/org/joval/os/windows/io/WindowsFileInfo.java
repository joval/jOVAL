// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.IOException;

import org.joval.io.fs.DefaultMetadata;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.io.IWindowsFileInfo;

/**
 * Implements extended attributes of a file on Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileInfo extends DefaultMetadata implements IWindowsFileInfo {
    private IWindowsFileInfo extended;

    public WindowsFileInfo(Type type, String path, String canonicalPath, long ctime, long mtime, long atime, long length,	
		int winType, IACE[] aces) {

	this(type, path, canonicalPath, ctime, mtime, atime, length, new ExtendedImpl(winType, aces));
    }

    public WindowsFileInfo(Type type, String path, String canonicalPath, long ctime, long mtime, long atime, long length,
		IWindowsFileInfo info) {

	super(type, path, null, canonicalPath, ctime, mtime, atime, length);
	extended = info;
    }

    // Implement IWindowsFileInfo

    /**
     * Returns one of the FILE_TYPE_ constants.
     */
    public int getWindowsFileType() throws IOException {
	return extended.getWindowsFileType();
    }

    public IACE[] getSecurity() throws IOException {
	return extended.getSecurity();
    }

    // Private

    static class ExtendedImpl implements IWindowsFileInfo {
	private int winType;
	private IACE[] aces;

	ExtendedImpl(int winType, IACE[] aces) {
	    this.winType = winType;
	    this.aces = aces;
	}

	// Implement IWindowsFileInfo

	public int getWindowsFileType() {
	    return winType;
	}

	public IACE[] getSecurity() {
	    return aces;
	}
    }
}
