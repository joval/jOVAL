// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.powershell;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * An interface to a powershell runspace pool.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRunspacePool {
    /**
     * Returns all the currently open runspaces.
     */
    Collection<IRunspace> enumerate();

    /**
     * Returns the maximum number of runspaces that can live in the pool.
     */
    int capacity();

    /**
     * Get a specific runspace, given its ID.
     *
     * @throws NoSuchElementException if no runspace with the given ID was found in the pool.
     */
    IRunspace get(String id) throws NoSuchElementException;

    /**
     * Create (and return) a new Runspace in the pool.
     *
     * @throws IndexOutOfBoundsException if the pool is already at capacity.
     */
    IRunspace spawn() throws Exception;
}
