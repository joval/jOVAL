// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.intf.util.ILoggable;

/**
 * Representation of tree of nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITree extends ILoggable {
    /**
     * Get the root node of the tree.
     */
    public INode getRoot();

    /**
     * Get a node from the tree.
     *
     * @throws NoSuchElementException if there is no node with the specified name.
     */
    public INode lookup(String path) throws NoSuchElementException;

    /**
     * Get the delimiter of node names in a path.
     */
    public String getDelimiter();

    /**
     * Recursively search this node for children with paths patching the given pattern.  Note that this node may itself be
     * included in the results.  Returns a list of paths.
     */
    public Collection<String> search(Pattern p, boolean followLinks);
}
