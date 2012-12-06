// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import com.sun.jna.platform.win32.Kernel32Util;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.util.JOVALMsg;

/**
 * The local IWindowsFilesystemDriver implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalDriver extends BaseDriver {
    /**
     * Create a new driver with the specified logger.
     */
    public LocalDriver(LocLogger logger) {
	super(logger);
    }

    // Implement IWindowsFilesystemDriver

    public Collection<IFilesystem.IMount> getMounts(Pattern filter) throws IOException {
	try {
	    Collection<IFilesystem.IMount> mounts = new ArrayList<IFilesystem.IMount>();
	    File[] roots = File.listRoots();
	    for (int i=0; i < roots.length; i++) {
		String path = roots[i].getPath();
		mounts.add(new WindowsMount(path, IWindowsFilesystem.FsType.typeOf(Kernel32Util.getDriveType(path))));
	    }
	    if (filter == null) {
		return mounts;
	    } else {
		Collection<IFilesystem.IMount> results = new ArrayList<IFilesystem.IMount>();
		for (IFilesystem.IMount mount : mounts) {
		    if (filter.matcher(mount.getType()).find()) {
			logger.info(JOVALMsg.STATUS_FS_MOUNT_SKIP, mount.getPath(), mount.getType());
		    } else {
			logger.info(JOVALMsg.STATUS_FS_MOUNT_ADD, mount.getPath(), mount.getType());
			results.add(mount);
		    } 
		}
		return results;
	    }
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new IOException(e);
	}
    }
}
