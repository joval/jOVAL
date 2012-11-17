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
    String DELIM_STR = "/";
    char DELIM_CH = '/';

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

    /**
     * Specifies a preload method wherein the output of the find command is staged in a file on the remote system.  If
     * stateful sessions are allowed, the contents of this file will be downloaded to the workspace and kept for MAXAGE.
     *
     * If session statefulness is not allowed, the cache file will be left on the remote machine potentially to be re-used,
     * for MAXAGE.  The benefit is that running find is expensive and can be subsequently avoided.  To prevent this, and
     * to always re-run find, simply use the STREAM_METHOD.
     *
     * In any case, if for some reason a subsequent scan involves a different set of filesystems, the cached result is
     * discarded and a new one computed.
     *
     */
    String VAL_FILE_METHOD = "file";

    /**
     * Specifies a preload method wherein the output of the find command is streamed down live to jOVAL.
     */
    String VAL_STREAM_METHOD = "stream";

    /**
     * Property governing the maximum age, in milliseconds, of the file storing the find results used by precaching.  This
     * is only relevant when the preload method is VAL_FILE_METHOD.
     */
    String PROP_PRELOAD_MAXAGE = "fs.preload.maxAge";

    /**
     * Returns the platform-specific driver for this filesystem.
     *
     * @see org.joval.intf.unix.io.IUnixFilesystemDriver
     */
    IUnixFilesystemDriver getDriver();
}
