// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A platform-independent abstraction of a server filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFilesystem {
    /**
     * Get the char delimiter for pathnames.
     */
    public char getDelimChar();

    /**
     * Get the String delimiter for pathnames.
     */
    public String getDelimString();

    /**
     * An interface with all the methods of java.io.File and jcifs.smb.SmbFile.
     */
    public IFile getFile(String path) throws IllegalArgumentException, IOException;

    /**
     * @param vol if true, the file is volatile and its attributes should not be cached.
     */
    public IFile getFile(String path, boolean vol) throws IllegalArgumentException, IOException;

    /**
     * Get random access to an IFile.
     */
    public IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException;

    /**
     * Get random access to a file given its path (such as would be passed into the getFile method).
     */
    public IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException;

    /**
     * Read a file.
     */
    public InputStream getInputStream(String path) throws IllegalArgumentException, IOException;

    /**
     * Write to a file.
     */
    public OutputStream getOutputStream(String path) throws IllegalArgumentException, IOException;

    /**
     * Optionally, append to a file.
     */
    public OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException;

    /**
     * Search for a path on the filesystem.
     *
     * @argument path contains [esacped] delimiter-delimited regex search patterns describing a directory hierarchy.
     * @returns a list of local paths patching the patterns.
     */
    public List<String> search(String path) throws IOException;
    public List<String> search(String parent, String path) throws IOException;
    public List<String> search(String parent, String path, boolean followLinks) throws IOException;
}
