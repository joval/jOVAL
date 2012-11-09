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
    String DELIM_STR = "/";
    char DELIM_CH = '/';

    /**
     * Property governing the method used when preloading the cache.  Valid methods are FILE_METHOD and STREAM_METHOD.
     */
    String PROP_PRELOAD_METHOD = "fs.preload.method";

    /**
     * Property governing the threshold used to automatically preload the cache for remote IUnixFilesystem implementations.
     * Set to 0 to always preload the cache, -1 to never preload.  A "good" value is around 100.
     */
    String PROP_PRELOAD_TRIGGER = "fs.preload.remote.threshold";

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
