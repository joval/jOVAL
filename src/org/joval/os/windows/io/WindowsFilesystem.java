// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Win32Exception;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.io.fs.CacheFilesystem;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.util.tree.Node;
import org.joval.util.tree.TreeHash;

/**
 * The local IFilesystem implementation for Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFilesystem extends CacheFilesystem implements IWindowsFilesystem {
    private static Collection<String> mounts;

    private int entries = 0, maxEntries = 0;
    private boolean preloaded = false;

    public WindowsFilesystem(IBaseSession session, IEnvironment env, IPathRedirector redirector) {
	super(session, env, redirector, File.separator);
	if (mounts == null) {
	    try {
		String filterStr = props.getProperty(PROP_MOUNT_FSTYPE_FILTER);
		if (filterStr == null) {
		    mounts = getMounts(null);
		} else {
		    mounts = getMounts(Pattern.compile(filterStr));
		}
	    } catch (Exception e) {
		logger.warn(JOVALMsg.ERROR_MOUNT);
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    @Override
    protected IFile accessResource(String path, int flags) throws IllegalArgumentException, IOException {
        if (autoExpand) {
            path = env.expand(path);
        }
        String realPath = path;
        if (redirector != null) {
            String alt = redirector.getRedirect(path);
            if (alt != null) {
                realPath = alt;
            }
        }

        if (isValidPath(realPath)) {
            return new WindowsFile(this, new File(realPath), path);
	} else if (isDrive(realPath)) {
            return new WindowsFile(this, new File(realPath + DELIM_STR), path);
        }
        throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
    }

    @Override
    public boolean loadCache() {
	if (!props.getBooleanProperty(PROP_PRELOAD_LOCAL)) {
	    return false;
	} else if (preloaded) {
	    return true;
	} else if (mounts == null) {
	    return false;
	}

	//
	// Wipe out any existing cache contents before starting.
	//
	reset();
	entries = 0;
	maxEntries = props.getIntProperty(PROP_PRELOAD_MAXENTRIES);
	try {
	    for (String mount : mounts) {
		addRecursive(new File(mount));
	    }
	    preloaded = true;
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_DONE, cacheSize());
	    return true;
	} catch (PreloadOverflowException e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD);
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    @Override
    public String getDelimiter() {
	return DELIM_STR;
    }

    public boolean isLocalPath(String path) {
	if (isValidPath(path)) {
	    return mounts.contains(path.substring(0, 3));
	} else {
	    return false;
	}
    }

    // Private

    private int count = 0;

    private void addRecursive(File f) throws PreloadOverflowException, IOException {
System.out.println("DAS addRecursive " + f.toString());
	if (++count % 20000 == 0) {
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_FILE_PROGRESS, count);
	}
	if (entries++ < maxEntries) {
	    String path = f.getCanonicalPath();
	    if (!path.equals(f.getPath())) {
		logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, path); // skip links
	    } else {
		addToCache(path, new WindowsFile(this, f, path));
		if (f.isDirectory()) {
		    File[] children = f.listFiles();
		    if (children != null) {
			for (File child : f.listFiles()) {
			    addRecursive(child);
			}
		    }
		}
	    }
	} else {
	    throw new PreloadOverflowException();
	}
    }

    private boolean isValidPath(String s) {
	if (s.length() >= 3) {
            return StringTools.isLetter(s.charAt(0)) && s.charAt(1) == ':' && s.charAt(2) == DELIM_CH;
	}
	return false;
    }

    private boolean isDrive(String s) {
	if (s.length() == 2) {
            return StringTools.isLetter(s.charAt(0)) && s.charAt(1) == ':';
	}
	return false;
    }

    private Collection<String> getMounts(Pattern filter) throws Win32Exception {
	Collection<String> mounts = new Vector<String>();
	File[] roots = File.listRoots();
	for (int i=0; i < roots.length; i++) {
	    String name = roots[i].getPath();
	    FsType type = FsType.typeOf(Kernel32Util.getDriveType(name));
	    if (filter != null && filter.matcher(type.value()).find()) {
		logger.info(JOVALMsg.STATUS_FS_MOUNT_SKIP, name, type.value());
	    } else {
		logger.info(JOVALMsg.STATUS_FS_MOUNT_ADD, name, type.value());
		mounts.add(name);
	    }
	}
	return mounts;
    }
}
