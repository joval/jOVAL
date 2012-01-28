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
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.io.BaseFilesystem;
import org.joval.util.tree.CachingTree;
import org.joval.util.tree.Tree;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * The local IFilesystem implementation for Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFilesystem extends BaseFilesystem implements IWindowsFilesystem {
    private int entries = 0, maxEntries = 0;
    private boolean preloaded;

    public WindowsFilesystem(IBaseSession session, IEnvironment env, IPathRedirector redirector) {
	super(session, env, redirector);
    }

    /**
     * @override
     */
    public IFile getFile(String path) throws IllegalArgumentException, IOException {
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
        if (realPath.length() > 2 && realPath.charAt(1) == ':') {
            if (StringTools.isLetter(realPath.charAt(0))) {
                return new WindowsFile(new FileProxy(this, new File(realPath), path));
            }
        }
        throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
    }

    public boolean preload() {
	if (!props.getBooleanProperty(PROP_PRELOAD_LOCAL)) {
	    return false;
	} else if (preloaded) {
	    return true;
	}

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
		String name = roots[i].getName();
		FsType type = FsType.typeOf(Kernel32Util.getDriveType(name));
		if (filter.contains(type.value())) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, name);
		} else {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, name);
		    ITreeBuilder tree = cache.getTreeBuilder(name);
		    if (tree == null) {
			tree = new Tree(name, getDelimiter());
			cache.addTree(tree);
		    }
		    addRecursive(tree, roots[i]);
		}
	    }
	    preloaded = true;
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

    private void addRecursive(ITreeBuilder tree, File f) throws PreloadOverflowException, IOException {
	if (entries++ < maxEntries) {
	    String path = f.getCanonicalPath();
	    if (!path.equals(f.getPath())) {
		logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, path); // skip links
	    } else if (f.isFile()) {
		INode node = tree.getRoot();
		try {
		    while ((path = trimToken(path, getDelimiter())) != null) {
			node = node.getChild(getToken(path, getDelimiter()));
		    }
		} catch (UnsupportedOperationException e) {
		    do {
			node = tree.makeNode(node, getToken(path, getDelimiter()));
		    } while ((path = trimToken(path, getDelimiter())) != null);
		} catch (NoSuchElementException e) {
		    do {
			node = tree.makeNode(node, getToken(path, getDelimiter()));
		    } while ((path = trimToken(path, getDelimiter())) != null);
		}
	    } else if (f.isDirectory()) {
		File[] children = f.listFiles();
		if (children == null) {
		    logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, path);
		} else {
		    for (File child : children) {
			addRecursive(tree, child);
		    }
		}
	    } else {
		logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, path);
	    }
	} else {
	    throw new PreloadOverflowException();
	}
    }
}
