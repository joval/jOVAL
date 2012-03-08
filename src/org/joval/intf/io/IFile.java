// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.joval.intf.util.tree.ICacheable;

/**
 * A platform-independent abstraction of a File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFile extends ICacheable {
    int NOCACHE		= 0;
    int READVOLATILE	= 1;
    int READONLY	= 4;
    int READWRITE	= 6;

    /**
     * Get the time that the file was last accessed.
     */
    public long accessTime() throws IOException;

    /**
     * Get the time that the file was created.
     */
    public long createTime() throws IOException;

    /**
     * Does the file exist?
     */
    public boolean exists() throws IOException;

    /**
     * Create a directory at this IFile's path.
     */
    public boolean mkdir();

    /**
     * Get an InputStream of the file contents.
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Get an OutputStream to the file.  Start at the beginning (overwrite) or the end (append) as dictated.
     */
    public OutputStream getOutputStream(boolean append) throws IOException;

    /**
     * Get random access.
     */
    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException;

    /**
     * Does this file represent a directory?  Note, if this file is a link to a directory, this method is intended to
     * return true.
     */
    public boolean isDirectory() throws IOException;

    /**
     * Does this file represent a regular file (i.e., not a directory)?
     */
    public boolean isFile() throws IOException;

    /**
     * Get the time this file was last modified.
     */
    public long lastModified() throws IOException;

    /**
     * Get the size (in bytes) of this file.
     */
    public long length() throws IOException;

    /**
     * For a directory, list the names of the subdirectories.
     */
    public String[] list() throws IOException;

    /**
     * For a directory, lists all the child files.
     */
    public IFile[] listFiles() throws IOException;

    /**
     * For a directory, lists all the child files whose names match the specified pattern.
     */
    public IFile[] listFiles(Pattern p) throws IOException;

    /**
     * Delete the file.
     */
    public void delete() throws IOException;

    /**
     * Returns the full path as it appears to the system on which the IFile resides (not necessarily canonical).
     */
    public String getPath();

    /**
     * Returns the canonical path representation of the IFile (i.e., linkless path).
     */
    public String getCanonicalPath();

    /**
     * Get the name of the file.
     */
    public String getName();

    /**
     * Returns a unique representation of the IFile. This might be a local name, a URL or a UNC pathname.
     */
    public String toString();
}
