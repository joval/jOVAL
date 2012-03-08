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

import com.sun.jna.platform.win32.Kernel32Util;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.io.BaseFilesystem;
import org.joval.io.FileProxy;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.tree.Node;
import org.joval.util.tree.TreeHash;

/**
 * The local IFilesystem implementation for Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFilesystem extends BaseFilesystem implements IWindowsFilesystem {
    private int entries = 0, maxEntries = 0;
    private boolean preloaded = false;

    public WindowsFilesystem(IBaseSession session, IEnvironment env, IPathRedirector redirector) {
	super(session, env, redirector, File.separator);
    }

    @Override
    protected IFile accessResource(String path) throws IllegalArgumentException, IOException {
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
	    if (isDrive(realPath)) {
                return new WindowsFile(new FileProxy(this, new File(realPath + delimiter), path));
	    } else {
                return new WindowsFile(new FileProxy(this, new File(realPath), path));
            }
        }
        throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
    }

    @Override
    protected String[] listChildren(String path) throws Exception {
	File f = ((WindowsFile)accessResource(path)).getFile().getFile();
	if (f.isDirectory()) {
	    return f.list();
	} else {
	    return null;
	}
    }

    @Override
    public boolean loadCache() {
	if (!props.getBooleanProperty(PROP_PRELOAD_LOCAL)) {
	    return false;
	} else if (preloaded) {
	    return true;
	}

	//
	// Wipe out any existing cache contents.
	//
	reset();

	entries = 0;
	maxEntries = props.getIntProperty(PROP_PRELOAD_MAXENTRIES);
	try {
	    Collection<String> filter = new Vector<String>();
	    String filterStr = props.getProperty(PROP_PRELOAD_FSTYPE_FILTER);
	    if (filterStr != null) {
		filter.addAll(StringTools.toList(StringTools.tokenize(filterStr, ":", true)));
	    }

	    File[] roots = File.listRoots();
	    for (int i=0; i < roots.length; i++) {
		String name = roots[i].getPath();
		FsType type = FsType.typeOf(Kernel32Util.getDriveType(name));
		if (filter.contains(type.value())) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, name, type.value());
		} else {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, name, type.value());
		    addRecursive(roots[i]);
		}
	    }
	    preloaded = true;
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_DONE, countCacheItems());
	    return true;
	} catch (PreloadOverflowException e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD);
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    // Private

    private int count = 0;

    private void addRecursive(File f) throws PreloadOverflowException, IOException {
	if (++count % 20000 == 0) {
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_FILE_PROGRESS, count);
	}
	if (entries++ < maxEntries) {
	    String path = f.getCanonicalPath();
	    if (!path.equals(f.getPath())) {
		logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, path); // skip links
	    } else {
		FileProxy fp = new FileProxy(this, path);
		fp.isfile = f.isFile();
		fp.isdir = f.isDirectory();
		addToCache(path, new WindowsFile(fp));
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
	if (s.length() >= 2) {
            return StringTools.isLetter(s.charAt(0)) && s.charAt(1) == ':';
	}
	return false;
    }

    private boolean isDrive(String s) {
	if (s.length() == 2) {
            return isValidPath(s);
	}
	return false;
    }
}
