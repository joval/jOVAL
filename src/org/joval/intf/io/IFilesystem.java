// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

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
     * Property specifying a list of filesystem types that should not be preloaded by an IFilesystem implementation.
     * Delimiter is the ':' character.
     */
    String PROP_MOUNT_FSTYPE_FILTER = "fs.localMount.filter";

    /**
     * Property governing whether the filesystem cache layer should be JDBM-backed (true) or memory-backed (false).
     */
    String PROP_CACHE_JDBM = "fs.cache.useJDBM";

    /**
     * Condition field for a type (i.e., file/directory/link).
     */
    int FIELD_FILETYPE = 50;

    /**
     * Condition field for a file path pattern.
     */
    int FIELD_PATH = 51;

    /**
     * Condition field for a file dirname (directory path) pattern. For files of type FILETYPE_DIR, the dirname is
     * the same as the path.
     */
    int FIELD_DIRNAME = 52;

    /**
     * Condition field for a file basename (filename) pattern. Files of type FILETYPE_DIR have no basename.
     */
    int FIELD_BASENAME = 53;

    String FILETYPE_FILE = "f";
    String FILETYPE_DIR = "d";
    String FILETYPE_LINK = "l";

    /**
     * A search condition for only matching directories.
     */
    ISearchable.ICondition DIRECTORIES = new ISearchable.ICondition() {
	public int getType() { return ISearchable.TYPE_EQUALITY; }
	public int getField() { return FIELD_FILETYPE; }
	public Object getValue() { return FILETYPE_DIR; }
    };

    /**
     * Get the path delimiter character used by this filesystem.
     */
    String getDelimiter();

    /**
     * Access an ISearchable for the filesystem.
     */
    ISearchable<IFile> getSearcher() throws IOException;

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
    String[] guessParent(Pattern p);

    /**
     * List the mounts on this filesystem, whose types do not match the specified typeFilter. Typically, for example,
     * a type filter might be used to exclude network mounts. Use null for no filtering.
     */
    OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException;
}
