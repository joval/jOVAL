// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.io;

import java.io.IOException;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.ISearchable;
import org.joval.intf.util.ISearchable.GenericCondition;
import org.joval.intf.util.ISearchable.ICondition;

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
     * The ICondition signifying that links should be followed in filesystem searches. The default behavior, if this
     * condition is not present, is to not follow links.
     */
    ICondition FOLLOW_LINKS = new GenericCondition(FIELD_FOLLOW_LINKS, ISearchable.TYPE_EQUALITY, Boolean.TRUE);

    /**
     * Condition field for the xdev flag (remain on filesystem).
     */
    int FIELD_XDEV = 101;

    /**
     * The ICondition signifying that the search should be confined to the filesystem of the FROM condition. If this
     * condition is not present, the search can include results that reside in linked filesystems.
     */
    ICondition XDEV = new GenericCondition(FIELD_XDEV, ISearchable.TYPE_EQUALITY, Boolean.TRUE);

    String DELIM_STR = "/";
    char DELIM_CH = '/';

    /**
     * Returns the platform-specific driver for this filesystem.
     *
     * @see org.joval.intf.unix.io.IUnixFilesystemDriver
     */
    IUnixFilesystemDriver getDriver();
}
