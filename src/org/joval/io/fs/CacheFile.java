// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.tree.INode;
import org.joval.util.CachingHierarchy;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * An IFile base class that works with a CacheFilesystem. Subclasses need only implement the getAccessor method, and
 * extend/implement a FileAccessor class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CacheFile implements IFile, Cloneable {
    private String path;

    protected CacheFilesystem fs;
    protected FileAccessor accessor;
    protected FileInfo info;

    protected CacheFile(CacheFilesystem fs, String path) {
	this.fs = fs;
	if (path.endsWith(fs.getDelimiter())) {
	    this.path = path.substring(0, path.lastIndexOf(fs.getDelimiter()));
	} else {
	    this.path = path;
	}
	accessor = null;
    }

    public abstract FileAccessor getAccessor();

    protected final boolean isRoot() {
	return path.length() == 0;
    }

    @Override
    public String toString() {
	return getPath();
    }

    // Implement ICacheable

    public final boolean exists() {
	if (info == null) {
	    if (getAccessor().exists()) {
		try {
		    info = accessor.getInfo();
		    return true;
		} catch (IOException e) {
		    return false;
		}
	    } else {
		return false;
	    }
	} else {
	    return true;
	}
    }

    public final boolean isLink() {
	if (exists()) {
	    return info.type == FileInfo.Type.LINK;
	} else {
	    return false;
	}
    }

    public final boolean isAccessible() {
	return accessor != null;
    }

    public final boolean isContainer() {
	try {
	    return isDirectory();
	} catch (IOException e) {
	}
	return false;
    }

    public final String getLinkPath() {
	if (isLink()) {
	    try {
		return getCanonicalPath();
	    } catch (IOException e) {
		return null;
	    }
	}
	throw new IllegalStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_CACHE_NOT_LINK, getPath()));
    }

    // Implement IFile

    public final String getFSName() throws IOException {
	try {
	    return fs.peek(path).getTree().getName();
	} catch (NoSuchElementException e) {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final long accessTime() throws IOException {
	if (exists()) {
	    return info.atime;
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final long createTime() throws IOException {
	if (exists()) {
	    return info.ctime;
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final boolean mkdir() {
	return getAccessor().mkdir();
    }

    public final InputStream getInputStream() throws IOException {
	if (exists()) {
	    return getAccessor().getInputStream();
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final OutputStream getOutputStream(boolean append) throws IOException {
	return getAccessor().getOutputStream(append);
    }

    public final IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	return getAccessor().getRandomAccess(mode);
    }

    public final boolean isDirectory() throws IOException {
	if (exists()) {
	    switch(info.type) {
	      case DIRECTORY:
		return true;

	      case LINK:
		return fs.getFile(getCanonicalPath()).isDirectory();

	      default:
		return false;
	    }
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final boolean isFile() throws IOException {
	if (exists()) {
	    switch(info.type) {
	      case DIRECTORY:
		return false;

	      case LINK:
		return fs.getFile(getCanonicalPath()).isFile();

	      default:
		return true;
	    }
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final long lastModified() throws IOException {
	if (exists()) {
	    return info.mtime;
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    public final long length() throws IOException {
	if (exists()) {
	    return info.length;
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }

    /**
     * Attempt to list from the cache, and if that fails, from the accessor.
     */
    public final String[] list() throws IOException {
	try {
	    INode node = fs.peek(getPath());
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
	    return fs.listChildren(getPath());
	} catch (Exception e) {
	    if (e instanceof IOException) {
		throw (IOException)e;
	    } else {
		throw new IOException(e);
	    }
	}
    }

    public final IFile[] listFiles() throws IOException {
	return listFiles(null);
    }

    public final IFile[] listFiles(Pattern p) throws IOException {
	String[] children = list();
	if (children == null) {
	    throw new IOException(JOVALSystem.getMessage(JOVALMsg.ERROR_IO_DIR_LISTING));
	}
	Vector<IFile> files = new Vector<IFile>();
	for (int i=0; i < children.length; i++) {
	    if (p == null || p.matcher(children[i]).find()) {
		files.add(fs.getFile(path + fs.getDelimiter() + children[i]));
	    }
	}
	return files.toArray(new IFile[files.size()]);
    }

    public final IFile getChild(String name) throws IOException {
	if (name.equals(".")) {
	    return this;
	} else if (name.equals("..")) {
	    return fs.getFile(getParent());
	} else {
	    for (IFile child : listFiles()) {
		if (name.equals(child.getName())) {
		    return child;
		}
	    }
	    throw new FileNotFoundException(path + fs.getDelimiter() + name);
	}
    }

    public final void delete() throws IOException {
	getAccessor().delete();
    }

    public final String getPath() {
	if (isRoot()) {
	    return fs.getDelimiter();
	} else {
	    return path;
	}
    }

    public final String getName() {
	if (isRoot()) {
	    return getPath();
	} else {
	    return path.substring(path.lastIndexOf(fs.getDelimiter()) + fs.getDelimiter().length());
	}
    }

    public final String getParent() {
	if (isRoot()) {
	    return getPath();
	} else {
	    return path.substring(0, path.lastIndexOf(fs.getDelimiter()));
	}
    }

    public final IFileEx getExtended() throws IOException {
	if (exists()) {
	    return info;
	} else {
	    throw new FileNotFoundException(getPath());
	}
    }
}
