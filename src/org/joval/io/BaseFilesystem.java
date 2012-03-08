// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.util.CachingHierarchy;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.tree.Node;

/**
 * The base class for IFilesystem implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFilesystem extends CachingHierarchy<IFile> implements IFilesystem {
    protected boolean autoExpand = true;
    protected String delimiter;
    protected IProperty props;
    protected IBaseSession session;
    protected IEnvironment env;
    protected IPathRedirector redirector;

    protected BaseFilesystem(IBaseSession session, IEnvironment env, IPathRedirector redirector, String delimiter) {
	super(session.getHostname(), delimiter);
	this.session = session;
	this.env = env;
	this.redirector = redirector;
	this.delimiter = delimiter;
	props = session.getProperties();
	setLogger(session.getLogger());
    }

public void save(File f) {
    super.save(f);
}

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement methods abstract in CachingHierarchy

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
	if (realPath.length() > 0 && realPath.charAt(0) == File.separatorChar) {
	    return new FileProxy(this, new File(realPath), path);
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
	}
    }

    protected String[] listChildren(String path) throws Exception {
	return ((FileProxy)accessResource(path)).getFile().list();
    }

    protected boolean loadCache() {
	return false;
    }

    // Implement IFilesystem

    public String getDelimiter() {
	return delimiter;
    }

    public IFile getFile(String path) throws IOException {
	return getFile(path, IFile.READONLY);
    }

    public IFile getFile(String path, int flags) throws IllegalArgumentException, IOException {
	try {
	    switch(flags) {
	      case IFile.NOCACHE:
	      case IFile.READWRITE:
	      case IFile.READVOLATILE:
		return accessResource(path);

	      case IFile.READONLY:
		return getResource(path);

	      default:
		throw new IllegalArgumentException(Integer.toString(flags));
	    }
	} catch (Exception e) {
	    if (e instanceof IOException) {
		throw (IOException)e;
	    } else if (e instanceof IllegalArgumentException) {
		throw (IllegalArgumentException)e;
	    } else {
		throw new IOException(e);
	    }
	}
    }

    public IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return file.getRandomAccess(mode);
    }

    public IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	return getFile(path).getRandomAccess(mode);
    }

    public InputStream getInputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getInputStream();
    }

    public OutputStream getOutputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(false);
    }

    public OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(append);
    }

    // Inner classes

    protected class PreloadOverflowException extends Exception {
	public PreloadOverflowException() {
	    super();
	}
    }
}
