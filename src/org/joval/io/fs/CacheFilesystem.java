// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.File;
import java.io.FileNotFoundException;
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
 * A CacheFilesystem is a CachingHierarchy that implements the IFilesystem interface.  It works with CacheFile
 * objects.  A CacheFilesystem will automatically store information about accessed files in memory, in order to
 * minimize traffic with the target machine.
 *
 * All IFilesystem implementations extend this base class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CacheFilesystem extends CachingHierarchy<IFile> implements IFilesystem {
    protected boolean autoExpand = true;
    protected IProperty props;
    protected IBaseSession session;
    protected IEnvironment env;
    protected IPathRedirector redirector;

    protected CacheFilesystem(IBaseSession session, IEnvironment env, IPathRedirector redirector, String delimiter) {
	super(session.getHostname(), delimiter);
	this.session = session;
	this.env = env;
	this.redirector = redirector;
	props = session.getProperties();
	setLogger(session.getLogger());
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement methods abstract in CachingHierarchy

    protected boolean loadCache() {
	return false;
    }

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
	if (realPath.length() > 0 && realPath.charAt(0) == File.separatorChar) {
	    return new DefaultFile(this, new File(realPath), path);
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
	}
    }

    protected int getDefaultFlags() {
	return IFile.READONLY;
    }

    protected final String[] listChildren(String path) throws Exception {
	return ((CacheFile)accessResource(path, getDefaultFlags())).getAccessor().list();
    }

    // Implement IFilesystem

    public final IFile getFile(String path) throws IOException {
	return getFile(path, getDefaultFlags());
    }

    public final IFile getFile(String path, int flags) throws IllegalArgumentException, IOException {
	try {
	    switch(flags) {
	      case IFile.NOCACHE:
	      case IFile.READWRITE:
	      case IFile.READVOLATILE:
		return accessResource(path, flags);

	      case IFile.READONLY:
		IFile f = getResource(path);
		if (f.exists()) {
		    if (f.getPath().equals(path)) {
			return f;
		    } else {
			//
			// Resources are cached according to canonical path, so an alias wrapper is used to provide
			// the expected pathname if it is different.
			//
			return new AliasFile((CacheFile)f, path);
		    }
		} else {
		    throw new FileNotFoundException(path);
		}

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

    public final IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return file.getRandomAccess(mode);
    }

    public final IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	return getFile(path).getRandomAccess(mode);
    }

    public final InputStream getInputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getInputStream();
    }

    public final OutputStream getOutputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(false);
    }

    public final OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(append);
    }

    // Inner classes

    protected final class PreloadOverflowException extends Exception {
	public PreloadOverflowException() {
	    super();
	}
    }

    // Private

    private class AliasFile extends CacheFile {
	private CacheFile base;

	AliasFile(CacheFile base, String aliasPath) {
	    super(base.fs, aliasPath);
	    this.base = base;
	    info = base.info;
	}

	public FileAccessor getAccessor() {
	    return base.getAccessor();
	}

	public String getCanonicalPath() {
	    //
	    // The path of the base CacheFile should be canonical, since it was returned from the cache.
	    //
	    return base.getPath();
	}
    }
}
