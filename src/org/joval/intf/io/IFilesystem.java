// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.joval.intf.util.tree.ITree;

/**
 * A platform-independent abstraction of a server filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFilesystem extends ITree {
    /**
     * Property governing the method used when preloading the cache.  Valid methods are FILE_METHOD and STREAM_METHOD.
     */
    String PROP_PRELOAD_METHOD = "fs.preload.method";

    String VAL_FILE_METHOD      = "file";
    String VAL_STREAM_METHOD    = "stream";

    /**
     * Property governing the maximum number of paths to pre-load into the filesystem map cache.
     */
    String PROP_PRELOAD_MAXENTRIES = "fs.preload.maxEntries";

    /**
     * Property governing the maximum age, in milliseconds, of the file storing the find results used by precaching.  This
     * is only relevant when the preload method is VAL_FILE_METHOD.
     */
    String PROP_PRELOAD_MAXAGE = "fs.preload.maxAge";

    /**
     * Property specifying a list of filesystem types that should not be preloaded by an IFilesystem implementation.
     * Delimiter is the ':' character.
     */
    public static final String PROP_PRELOAD_FSTYPE_FILTER = "fs.preload.filter";

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
    public static final String PROP_PRELOAD_REMOTE = "fs.preload.remote";

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
}
