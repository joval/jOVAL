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
     * Condition field for the link-following flag.
     */
    int FIELD_FOLLOW_LINKS = 100;

    /**
     * Condition field for the xdev flag (remain on filesystem).
     */
    int FIELD_XDEV = 101;

    String DELIM_STR = "/";
    char DELIM_CH = '/';

    /**
     * Returns the platform-specific driver for this filesystem.
     *
     * @see org.joval.intf.unix.io.IUnixFilesystemDriver
     */
    IUnixFilesystemDriver getDriver();
}
