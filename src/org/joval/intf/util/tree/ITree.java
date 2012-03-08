// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.intf.util.ILoggable;

/**
 * Representation of tree of nodes. Nodes have pathnames, the discrete elements of which are separated by the tree's
 * delimiter.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITree extends INode {
    /**
     * Returns the number of non-virtual nodes in the tree (i.e., nodes beyond a link are not counted).
     */
    public int size();

    /**
     * Get a node from the tree.
     *
     * @throws NoSuchElementException if there is no node with the specified name.
     */
    public INode lookup(String path) throws NoSuchElementException;

    /**
     * Recursively search this tree for children with paths patching the given pattern.  The search scope is limited
     * to the tree.  Since a forest is also a tree, you can perform a search that follows links between trees by searching
     * the forest.
     */
    public Collection<String> search(Pattern p, boolean followLinks);
}
