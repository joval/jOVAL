// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.joval.intf.util.ISearchable;

/**
 * A platform-independent abstraction of a server filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFilesystem extends ISearchable {
    int SEARCH_FLAG_FOLLOW_LINKS = 1;

    /**
     * Property governing the maximum number of paths to pre-load into the filesystem map cache.
     */
    String PROP_PRELOAD_MAXENTRIES = "fs.preload.maxEntries";

    /**
     * Property specifying a list of filesystem types that should not be preloaded by an IFilesystem implementation.
     * Delimiter is the ':' character.
     */
    String PROP_MOUNT_FSTYPE_FILTER = "fs.localMount.filter";

    /**
     * Property governing the map cache pre-load behavior for local filesystems (true/false).
     *
     * If false, a local IFilesystem implementation will use the tree-search algorithm to resolve searches.  If true, it
     * will scan (and cache) all file paths on the entire filesystem, and then subsequently perform regular expression
     * matching directly on the paths.
     */
    String PROP_PRELOAD_LOCAL = "fs.preload.local";

    /**
     * Property governing the map cacle pre-load behavior for remotely-accessed filesystems (true/false).
     */
    String PROP_PRELOAD_REMOTE = "fs.preload.remote";

    /**
     * Property governing whether the filesystem cache layer should be JDBM-backed (true) or memory-backed (false).
     */
    String PROP_CACHE_JDBM = "fs.cache.useJDBM";

    /**
     * Return whether or not the path points to a file on the local machine.
     */
    boolean isLocalPath(String path);

    /**
     * Get the path delimiter character used by this filesystem.
     */
    String getDelimiter();

    /**
     * Retrieve an IFile with default (IFile.READONLY) access.
     */
    IFile getFile(String path) throws IOException;

    /**
     * An interface with all the methods of java.io.File and jcifs.smb.SmbFile.
     *
     * @arg flags IFile.READONLY, IFile.READWRITE, IFile.READVOLATILE, IFile.NOCACHE
     */
    IFile getFile(String path, int flags) throws IllegalArgumentException, IOException;

    /**
     * Get random access to an IFile.
     */
    IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException;

    /**
     * Get random access to a file given its path (such as would be passed into the getFile method).
     */
    IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException;

    /**
     * Read a file.
     */
    InputStream getInputStream(String path) throws IllegalArgumentException, IOException;

    /**
     * Write to a file.
     */
    OutputStream getOutputStream(String path) throws IllegalArgumentException, IOException;

    /**
     * Optionally, append to a file.
     */
    OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException;
}
