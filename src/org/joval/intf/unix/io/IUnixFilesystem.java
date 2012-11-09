// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.io;

import java.io.IOException;

import org.joval.intf.io.IFilesystem;

/**
 * Defines extended attributes of a filesystem on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixFilesystem extends IFilesystem {
    /**
     * Flag for the ISearchable specifying that links should be traversed while searching.
     */
    int FLAG_FOLLOW_LINKS = 4;

    /**
     * Don't search outside the filesystem of the starting point.
     */
    int FLAG_XDEV = 8;

    String DELIM_STR = "/";
    char DELIM_CH = '/';

    /**
     * Returns the platform-specific driver for this filesystem.
     *
     * @see org.joval.intf.unix.io.IUnixFilesystemDriver
     */
    IUnixFilesystemDriver getDriver();
}
