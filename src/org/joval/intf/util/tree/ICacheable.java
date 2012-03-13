// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

/**
 * An interface describing an object that can be cached by a HierarchicalCache.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ICacheable {
    /**
     * Returns whether the ICacheable should be cached (non-existent objects should not be cached).
     */
    boolean exists();

    /**
     * Returns whether or not the ICacheable has a handle to a live object, or if it only contains data about the object.
     */
    boolean isAccessible();

    /**
     * Returns whether the ICacheable is strictly a container for other ICacheable objects.
     */
    boolean isContainer();

    /**
     * Returns whether the ICacheable represents a link to another ICacheable.
     */
    boolean isLink();

    /**
     * If the ICacheable represents a link, returns a path to the link target.
     */
    String getLinkPath() throws IllegalStateException;
}
