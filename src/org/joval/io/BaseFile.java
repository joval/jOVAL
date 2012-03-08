// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.tree.INode;
import org.joval.util.CachingHierarchy;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Features common to all IFiles, primarily including the methods centered around listing of child files, which
 * leverages the cache, and failing that, the access methods defined in the class extending BaseFilesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFile implements IFile {
    protected BaseFilesystem fs;
    protected String path;
    protected boolean accessible;

    protected BaseFile(BaseFilesystem fs, String path) {
	this.fs = fs;
	if (path.endsWith(fs.getDelimiter())) {
	    this.path = path.substring(0, path.lastIndexOf(fs.getDelimiter()));
	} else {
	    this.path = path;
	}
	accessible = false;
    }

    protected boolean isRoot() {
	return path.length() == 0;
    }

    // Implement ICacheable (partially)

    public void setCachePath(String cachePath) {
	// no-op
    }

    public boolean isAccessible() {
	return accessible;
    }

    public boolean isContainer() {
	try {
	    return isDirectory();
	} catch (IOException e) {
	}
	return false;
    }

    public String getLinkPath() {
	throw new IllegalStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_CACHE_NOT_LINK, path));
    }

    // Implement IFile (partially)

    public String getPath() {
	return path;
    }

    public String getName() {
	int ptr = path.lastIndexOf(fs.getDelimiter());
	if (ptr == -1) {
	    return path;
	} else {
	    return path.substring(ptr + fs.getDelimiter().length());
	}
    }

    /**
     * Attempt to list from the cache, and if that fails, from the accessor.
     */
    public String[] list() throws IOException {
	try {
	    INode node = fs.peekInsideCache(getPath());
	    if (node.hasChildren()) {
		Collection<INode> children = node.getChildren();
		String[] sa = new String[children.size()];
		int i=0;
		for (INode child : children) {
		    sa[i++] = child.getName();
		}
		return sa;
	    }
	} catch (NoSuchElementException e) {
	}
	try {
	    if (isRoot()) {
		return fs.listChildren(fs.getDelimiter());
	    } else {
		return fs.listChildren(path);
	    }
	} catch (Exception e) {
	    if (e instanceof IOException) {
		throw (IOException)e;
	    } else {
		throw new IOException(e);
	    }
	}
    }

    public IFile[] listFiles() throws IOException {
	return listFiles(null);
    }

    public IFile[] listFiles(Pattern p) throws IOException {
	String[] children = list();
	if (children == null) {
	    throw new IOException(JOVALSystem.getMessage(JOVALMsg.ERROR_IO_DIR_LISTING));
	}
	Vector<IFile> files = new Vector<IFile>();
	for (int i=0; i < children.length; i++) {
	    if (p == null || p.matcher(children[i]).find()) {
		StringBuffer sb = new StringBuffer(path);
		if (!path.endsWith(fs.getDelimiter())) {
		    sb.append(fs.getDelimiter());
		}
		files.add(fs.getFile(sb.append(children[i]).toString()));
	    }
	}
	return files.toArray(new IFile[files.size()]);
    }
}
