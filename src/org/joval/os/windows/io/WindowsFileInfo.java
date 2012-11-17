// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.IOException;

import org.joval.io.AbstractFilesystem;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;

/**
 * Implements extended attributes of a file on Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileInfo extends AbstractFilesystem.FileInfo implements IWindowsFileInfo {
    private IWindowsFileInfo accessor;

    public WindowsFileInfo(long ctime, long mtime, long atime, Type type, long length, int winType, IACE[] aces) {
	super(ctime, mtime, atime, type, length);
	accessor = new InternalAccessor(winType, aces);
    }

    public WindowsFileInfo(long ctime, long mtime, long atime, Type type, long length, IWindowsFileInfo info) {
	super(ctime, mtime, atime, type, length);
	accessor = info;
    }

    // Implement IWindowsFileInfo

    /**
     * Returns one of the FILE_TYPE_ constants.
     */
    public int getWindowsFileType() throws IOException {
	return accessor.getWindowsFileType();
    }

    public IACE[] getSecurity() throws IOException {
	return accessor.getSecurity();
    }

    // Private

    class InternalAccessor implements IWindowsFileInfo {
	private int winType;
	private IACE[] aces;

	InternalAccessor(int winType, IACE[] aces) {
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
