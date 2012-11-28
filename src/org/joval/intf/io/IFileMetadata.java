// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Metadata about a file.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFileMetadata {
    enum Type {
	FILE,
	DIRECTORY,
	LINK;
    }

    long UNKNOWN_TIME = -1L;

    Type getType() throws IOException;

    /**
     * If the IFile represents a link, returns a path to the link target.
     */
    String getLinkPath() throws IllegalStateException, IOException;

    /**
     * Get the time that the file was last accessed.
     */
    public long accessTime() throws IOException;

    /**
     * Get the time that the file was created.
     */
    public long createTime() throws IOException;

    /**
     * Get the time this file was last modified.
     */
    public long lastModified() throws IOException;

    /**
     * Get the size (in bytes) of this file.
     */
    public long length() throws IOException;

    /**
     * Returns the full path as it appears to the system on which the IFile resides (not necessarily canonical).
     */
    public String getPath();

    /**
     * Returns the canonical path representation of the IFile (i.e., linkless path).
     */
    public String getCanonicalPath() throws IOException;

    /**
     * Get a platform-specific extended attributes API, if any.
     */
    public IFileEx getExtended() throws IOException;
}
