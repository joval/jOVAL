// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.io;

import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Pattern;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.ILoggable;

/**
 * An interface describing the platform-specific requirements for a UnixFilesystem driver.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixFilesystemDriver extends ILoggable {
    /**
     * Get a list of mount points.
     *
     * @arg typeFilter A regex pattern indicating the /types/ of filesystems to /exclude/ from the result.  Use null for
     *                 an unfiltered list of mount points.
     */
    public Collection<IFilesystem.IMount> getMounts(Pattern typeFilter) throws Exception;

    /**
     * Returns a string containing the correct find command for the Unix flavor.
     *
     * The resulting command will follow links, but restrict results to the originating filesystem.
     *
     * @param from the desired starting-point for the search
     * @param depth the maximum search depth, or -1 for unlimited
     * @param flags see the flags from ISearchable.search method.
     * @param pattern the pattern for filtering results
     */
    public String getFindCommand(String from, int maxDepth, int flags, String pattern);

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
    public IUnixFileInfo nextFileInfo(Iterator<String> lines);
}
