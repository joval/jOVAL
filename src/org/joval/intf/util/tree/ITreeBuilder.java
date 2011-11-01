// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

/**
 * Extension of ITree with the ability to create new nodes and links.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITreeBuilder extends ITree {
    /**
     * Add a node to the tree.
     */
    public INode makeNode(INode parent, String name);

    /**
     * Add a node to the tree, which is a link to another node.
     */
    public INode makeLink(INode parent, String name, String destinationPath);
}
