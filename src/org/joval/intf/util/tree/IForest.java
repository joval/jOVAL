// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.intf.util.ILoggable;

/**
 * A forest is a collection of trees. A tree is a structure with a name containing a hierarchical collection of nodes.
 * Every node in a tree has a path that begins with the name of its tree; the tree itself being the root node.
 *
 * No tree can contain a node whose path is identical to the name of another tree in the forest. This characteristic makes
 * a forest searchable for all its nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IForest extends ITree {
    /**
     * All the trees in the forest must use the same delimiter for node pathnames.
     */
    public String getDelimiter();

    /**
     * @returns the tree that was displaced, if any
     *
     * @throws IllegalArgumentException If the forest already contains a non-leaf node path matching the tree name.  If an
     *					existing leaf node path matches the name, it is excised from the original tree.
     *					Similarly, if the tree contains a node matching an existing tree name in the forest,
     *					it is excised if a leaf node, otherwise the exception is thrown.
     */
    public ITree addTree(ITree tree) throws IllegalArgumentException;

    /**
     * Get the tree with the specified name.
     */
    public ITree getTree(String name);
}
