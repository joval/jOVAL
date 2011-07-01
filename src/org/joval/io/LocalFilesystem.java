// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IPathRedirector;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IEnvironment;
import org.joval.util.JOVALSystem;

/**
 * A pure-Java implementation of the IFilesystem interface.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalFilesystem extends CachingFilesystem {
    private static boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private boolean autoExpand = true;
    private IEnvironment env;
    private IPathRedirector redirector;

    public LocalFilesystem (IEnvironment env, IPathRedirector redirector) {
	super();
	this.env = env;
	this.redirector = redirector;
	if (WINDOWS) {
	    setCaseInsensitive(true);
	}
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement IPathRedirector

    public String getRedirect(String path) {
	if (redirector == null) {
	    return path;
	} else {
	    return redirector.getRedirect(path);
	}
    }

    // Implement IFilesystem

    public char getDelimChar() {
	return File.separatorChar;
    }

    public String getDelimString() {
	return File.separator;
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = env.expand(path);
	}
	path = getRedirect(path);
	if (WINDOWS) {
	    if (path.length() > 2 && path.charAt(1) == ':') {
	        if (isLetter(path.charAt(0))) {
		    return new FileProxy(new File(path));
	        }
	    }
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_FS_LOCALPATH", path));
	} else if (path.charAt(0) == File.separatorChar) {
	    return new FileProxy(new File(path));
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
