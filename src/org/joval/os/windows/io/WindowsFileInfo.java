// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.IOException;

import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.io.fs.FileInfo;

/**
 * Implements extended attributes of a file on Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileInfo extends FileInfo implements IWindowsFileInfo {
    private IWindowsFileInfo provider;

    /**
     * Create a WindowsFile with a live IFile accessor.
     */
    public WindowsFileInfo(long ctime, long mtime, long atime, Type type, long length, IWindowsFileInfo provider) {
	super(ctime, mtime, atime, type, length);
	this.provider = provider;
    }

    // Implement IWindowsFileInfo

    /**
     * Returns one of the FILE_TYPE_ constants.
     */
    public int getWindowsFileType() throws IOException {
	return provider.getWindowsFileType();
    }

    public IACE[] getSecurity() throws IOException {
	return provider.getSecurity();
    }
}
