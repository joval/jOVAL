// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.cisco.system;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An interface for accessing data from the "show tech-support" IOS command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITechSupport {
    /**
     * The series of dashes appearing before and after descriptive section header text.
     */
    String DASHES = "------------------";

    String GLOBAL = "show running-config";

    /**
     * A list of subcommands for which information is available.
     */
    Collection<String> getShowSubcommands();

    /**
     * A complete list of all the "headings" for which information is available.  This includes all the show subcommands.
     */
    Collection<String> getHeadings();

    /**
     * Fetches the response lines associated with the given heading.
     *
     * @throws NoSuchElementException if the heading is not found.
     */
    List<String> getLines(String heading) throws NoSuchElementException;

    /**
     * Fetches the raw data associated with the given heading.
     *
     * @throws NoSuchElementException if the heading is not found.
     */
    String getData(String heading) throws NoSuchElementException;
}
