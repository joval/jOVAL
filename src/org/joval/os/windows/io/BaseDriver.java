// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.util.ILoggable;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.io.IWindowsFilesystemDriver;
import org.joval.os.windows.Timestamp;
import org.joval.os.windows.identity.ACE;
import org.joval.util.JOVALMsg;

/**
 * The base IWindowsFilesystemDriver implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseDriver implements IWindowsFilesystemDriver {
    protected LocLogger logger;

    /**
     * Create a new driver with the specified logger.
     */
    protected BaseDriver(LocLogger logger) {
	this.logger = logger;
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement (partially) IWindowsFilesystemDriver

    private static final String START   = "{";
    private static final String END     = "}";

    public IWindowsFileInfo nextFileInfo(Iterator<String> input) {
	boolean start = false;
	while(input.hasNext()) {
	    String line = input.next();
	    if (line.trim().equals(START)) {
		start = true;
		break;
	    }
	}
	if (start) {
	    long ctime=IFile.UNKNOWN_TIME, mtime=IFile.UNKNOWN_TIME, atime=IFile.UNKNOWN_TIME, len=-1L;
	    IFileMetadata.Type type = IFileMetadata.Type.FILE;
	    int winType = IWindowsFileInfo.FILE_TYPE_UNKNOWN;
	    Collection<IACE> aces = new ArrayList<IACE>();
	    String path = null;

	    while(input.hasNext()) {
		String line = input.next().trim();
		if (line.equals(END)) {
		    break;
		} else if (line.equals("Type: File")) {
		    winType = IWindowsFileInfo.FILE_TYPE_DISK;
		} else if (line.equals("Type: Directory")) {
		    type = IFileMetadata.Type.DIRECTORY;
		    winType = IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY;
		} else {
		    int ptr = line.indexOf(":");
		    if (ptr > 0) {
			String key = line.substring(0,ptr).trim();
			String val = line.substring(ptr+1).trim();
			if ("Path".equals(key)) {
			    path = val;
			} else {
			    try {
				if ("Ctime".equals(key)) {
				    ctime = Timestamp.getTime(new BigInteger(val));
				} else if ("Mtime".equals(key)) {
				    mtime = Timestamp.getTime(new BigInteger(val));
				} else if ("Atime".equals(key)) {
				    atime = Timestamp.getTime(new BigInteger(val));
				} else if ("Length".equals(key)) {
				    len = Long.parseLong(val);
				} else if ("ACE".equals(key)) {
				    aces.add(new ACE(val));
				} else if ("WinType".equals(key)) {
				    winType = Integer.parseInt(val);
				}
			    } catch (IllegalArgumentException e) {
				logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    }
			}
		    }
		}
	    }
	    return new WindowsFileInfo(type, path, path, ctime, mtime, atime, len, winType, aces.toArray(new IACE[0]));
	}
	return null;
    }
}
