// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.util.tree.INode;
import org.joval.intf.system.IEnvironment;
import org.joval.util.tree.CachingTree;
import org.joval.util.JOVALSystem;

/**
 * A pure-Java implementation of the IFilesystem interface.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalFilesystem extends CachingTree implements IFilesystem {
    private static boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private boolean autoExpand = true;
    private IEnvironment env;
    private IPathRedirector redirector;

    public LocalFilesystem (IEnvironment env, IPathRedirector redirector) {
	super();
	this.env = env;
	this.redirector = redirector;
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement methdos left abstract in CachingTree

    public boolean preload() {
	return false;
    }

    public String getDelimiter() {
	return File.separator;
    }

    public INode lookup(String path) throws NoSuchElementException {
	try {
	    IFile f = getFile(path);
	    if (f.exists()) {
		return f;
	    } else {
		throw new NoSuchElementException(path);
	    }
	} catch (IOException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_IO", e.getMessage()), e);
	    return null;
	}
    }

    // Implement IFilesystem

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = env.expand(path);
	}
	if (redirector != null) {
	    String alt = redirector.getRedirect(path);
	    if (alt != null) {
		path = alt;
	    }
	}
	if (WINDOWS) {
	    if (path.length() > 2 && path.charAt(1) == ':') {
	        if (isLetter(path.charAt(0))) {
		    return new FileProxy(this, new File(path));
	        }
	    }
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_FS_LOCALPATH", path));
	} else if (path.charAt(0) == File.separatorChar) {
	    return new FileProxy(this, new File(path));
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_FS_LOCALPATH", path));
	}
    }

    public IFile getFile(String path, boolean vol) throws IllegalArgumentException, IOException {
	return getFile(path);
    }

    public IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return new RandomAccessFileProxy(new RandomAccessFile(file.getLocalName(), mode));
    }

    public IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	return new RandomAccessFileProxy(new RandomAccessFile(path, mode));
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

    // Private

    /**
     * Check for ASCII values between [A-Z] or [a-z].
     */
    boolean isLetter(char c) {
        return (c >= 65 && c <= 90) || (c >= 95 && c <= 122);
    }
}
