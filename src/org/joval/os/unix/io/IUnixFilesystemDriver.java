// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.util.Iterator;
import java.util.List;

/**
 * An interface describing the platform-specific requirements for a UnixFilesystem driver.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixFilesystemDriver {
    /**
     * Get a list of mount points.  The result will be filtered by the configured list of filesystem types.
     */
    public List<String> listMounts(long timeout) throws Exception;

    /**
     * Returns a string containing the correct find command for the Unix flavor.  The command contains the String "%MOUNT%"
     * where the actual path of the mount should be substituted.
     *
     * The resulting command will follow links, but restrict results to the originating filesystem.
     */
    public String getFindCommand();

    /**
     * Returns some variation of the ls or stat command.  The final argument (not included) should be the escaped path of
     * the file being stat'd.
     */
    public String getStatCommand();

    /**
     * Generate a UnixFileInfo based on the output from the Stat command.  The lines iterator may contain output
     * representing one or more stat commands, but this method is expected to retrieve only the very next FileInfo.
     * If there are no more lines, this method should return null.
     */
    public UnixFileInfo nextFileInfo(Iterator<String> lines);
}
