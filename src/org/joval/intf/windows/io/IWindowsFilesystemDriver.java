// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.io;

import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Pattern;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;

/**
 * An interface describing the platform-specific requirements for a WindowsFilesystem driver.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWindowsFilesystemDriver extends ILoggable {
    /**
     * Get a list of mount points.
     *
     * @arg typeFilter A regex pattern indicating the /types/ of filesystems to /exclude/ from the result.  Use null for
     *                 an unfiltered list of mount points.
     */
    public Collection<IFilesystem.IMount> getMounts(Pattern typeFilter) throws Exception;

    /**
     * Generate a WindowsFileInfo based on the output from the Stat command.  The lines iterator may contain output
     * representing one or more stat commands, but this method is expected to retrieve only the very next FileInfo.
     * If there are no more lines, this method should return null.
     */
    public IWindowsFileInfo nextFileInfo(Iterator<String> lines);
}
