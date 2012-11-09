// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * An interface for searching a hierarchy.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISearchable<T> {
    /**
     * Value of flags with no settings at all.
     */
    int FLAG_NONE		= 0;

    /**
     * Flag indicating that links should be followed when searching.
     */
    int FLAG_FOLLOW_LINKS	= 1;

    /**
     * Flag indicating that only containers should be returned (i.e., branches, not leaves).
     */
    int FLAG_CONTAINERS		= 2;

    /**
     * Unlimited depth argument for recursive searches.
     */
    int DEPTH_UNLIMITED		= -1;

    /**
     * Recursively search for elements matching the given pattern.
     *
     * @param from starting point for the search (search happens below this path)
     * @param maxDepth the maximum number of hierarchies to traverse while searching. DEPTH_UNLIMITED for unlimited.
     * @param flags application-specific flags.
     * @param plugin @see ISearchable.ISearchPlugin
     */
    Collection<T> search(String from, Pattern p, int maxDepth, int flags, ISearchPlugin<T> plugin) throws Exception;

    /**
     * An interface specifying a command plug-in for a search.
     */
    public interface ISearchPlugin<T> {
	/**
	 * Get the plugin subcommand string.
	 */
	String getSubcommand();

	/**
	 * Generate an object of the specified type, given Iterator<String> input (generally, lines from a character stream).
	 *
	 * @return null when no more objects can be created from the input.
	 */
	T createObject(Iterator<String> input);
    }
}
