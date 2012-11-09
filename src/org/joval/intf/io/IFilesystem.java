// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.regex.Pattern;

import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;

/**
 * A platform-independent abstraction of a server filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFilesystem extends ILoggable {
    /**
     * Property governing whether a PATTERN_MATCH search of the filesystem will traverse symbolic links in order to
     * find matches. The OVAL specification is ambiguous about what the behavior should be.
     */
    String PROP_SEARCH_FOLLOW_LINKS = "fs.search.followLinks";

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
     * Property governing whether the filesystem cache layer should be JDBM-backed (true) or memory-backed (false).
     */
    String PROP_CACHE_JDBM = "fs.cache.useJDBM";

    /**
     * Get the path delimiter character used by this filesystem.
     */
    String getDelimiter();

    /**
     * Access an ISearchable for the filesystem.
     */
    ISearchable<IFile> getSearcher();

    /**
     * Get the default search plugin.
     */
    ISearchable.ISearchPlugin<IFile> getDefaultPlugin();

    /**
     * Retrieve an IFile with default (IFile.READONLY) access.
     */
    IFile getFile(String path) throws IOException;

    /**
     * Retrieve an IFile with the specified flags.
     *
     * @arg flags IFile.READONLY, IFile.READWRITE, IFile.READVOLATILE, IFile.NOCACHE
     */
    IFile getFile(String path, IFile.Flags flags) throws IOException;

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
     * Hazard a guess for the parent path of the specified pattern. Returns null if indeterminate.
     */
    String guessParent(Pattern p);

    /**
     * List the mounts on this filesystem, whose types do not match the specified typeFilter. Typically, for example,
     * a type filter might be used to exclude network mounts.
     */
    Collection<IMount> getMounts(Pattern typeFilter) throws IOException;

    /**
     * An interface describing a filesystem mount point.
     */
    public interface IMount {
	/**
	 * Get the path of the mount.
	 */
	String getPath();

	/**
	 * Get the type of the mount.
	 */
	String getType();
    }
}
