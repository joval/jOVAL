// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.juniper.system;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An interface for accessing data from the "request support information" JunOS command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISupportInformation {
    String GLOBAL = "show configuration | display set";

    /**
     * A list of subcommands for which information is available.
     */
    Collection<String> getShowSubcommands();

    /**
     * A complete list of all the "headings" for which information is available.  This includes all the show subcommands.
     */
    Collection<String> getHeadings();

    /**
     * Fetches the String data associates with the given heading.
     *
     * @throws NoSuchElementException if the heading is not found.
     */
    List<String> getData(String heading) throws NoSuchElementException;
}
