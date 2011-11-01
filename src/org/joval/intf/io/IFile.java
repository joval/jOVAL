// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.joval.intf.util.tree.INode;

/**
 * A platform-independent abstraction of a File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFile extends INode {
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
     * Does this file represent a directory?
     */
    public boolean isDirectory() throws IOException;

    /**
     * Does this file represent a regular file (i.e., not a directory)?
     */
    public boolean isFile() throws IOException;

    /**
     * Does this file represent a link?
     */
    public boolean isLink() throws IOException;

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
     * Delete the file.
     */
    public void delete() throws IOException;

    /**
     * Returns the full path as it appears to the system on which the IFile resides (not necessarily canonical).
     */
    public String getLocalName();

    public String getName();

    /**
     * Returns a unique representation of the IFile. This might be a local name, a URL or a UNC pathname.
     */
    public String toString();
}
