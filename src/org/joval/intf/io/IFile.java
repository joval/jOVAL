// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.joval.intf.util.tree.INode;

/**
 * A platform-independent abstraction of a File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFile extends INode {
    /**
     * Either the type of the specified file is unknown, or the function failed.
     */
    int FILE_TYPE_UNKNOWN = 0x0000;

    /**
     * The specified file is a disk file.
     */
    int FILE_TYPE_DISK = 0x0001;

    /**
     * The specified file is a character file, typically an LPT device or a console.
     */
    int FILE_TYPE_CHAR = 0x0002;

    /**
     * The specified file is a socket, a named pipe, or an anonymous pipe.
     */
    int FILE_TYPE_PIPE = 0x0003;

    /**
     * Unused.
     */
    int FILE_TYPE_REMOTE = 0x8000;

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
     * Returns one of the FILE_TYPE constants.
     */
    public int getFileType() throws IOException;

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
