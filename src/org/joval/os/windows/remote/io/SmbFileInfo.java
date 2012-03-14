// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.os.windows.io.WindowsFileInfo;

/**
 * Implements extended attributes of an SMB file, which always has an underlying accessor.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmbFileInfo extends WindowsFileInfo {
    private SmbAccessor accessor;

    /**
     * Create a WindowsFile with a live IFile accessor.
     */
    public SmbFileInfo(SmbAccessor accessor, Type type) {
	super(type, accessor);
	this.accessor = accessor;
    }

    // Caching happens at the JCIFS level, so no need to re-cache anything here.

    @Override
    public long getCtime() {
	try {
	    return accessor.getCtime();
	} catch (IOException e) {
	    return IFile.UNKNOWN_TIME;
	}
    }

    @Override
    public long getMtime() {
	try {
	    return accessor.getMtime();
	} catch (IOException e) {
	    return IFile.UNKNOWN_TIME;
	}
    }

    @Override
    public long getAtime() {
	try {
	    return accessor.getAtime();
	} catch (IOException e) {
	    return IFile.UNKNOWN_TIME;
	}
    }

    @Override
    public long getLength() {
	try {
	    return accessor.getLength();
	} catch (IOException e) {
	    return -1L;
	}
    }
}
