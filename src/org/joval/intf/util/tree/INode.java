// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Representation of a node on a tree.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface INode {
    /**
     * Get all the children of this node.
     *
     * @throws UnsupportedOperationException if the node is of Type.LEAF.
     * @throws NoSuchElementException if this is a node of Type.LINK, but the destination doesn't exist.
     */
    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException;

    /**
     * Get all the children of this node whose names match the specified Pattern.
     *
     * @throws UnsupportedOperationException if the node is of Type.LEAF.
     * @throws NoSuchElementException if this is a node of Type.LINK, but the destination doesn't exist.
     */
    public Collection<INode> getChildren(Pattern p) throws NoSuchElementException, UnsupportedOperationException;

    /**
     * Get a child with a specific name.
     *
     * @throws UnsupportedOperationException if the node is of Type.LEAF.
     * @throws NoSuchElementException if there is no child with the specified name.
     */
    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException;

    /**
     * Get the name of this node.
     */
    public String getName();

    /**
     * Get the path of this node, including links in the path hierarchy.
     */
    public String getPath();

    /**
     * Get the absolute path of this node, irrespective of how it was reached using links.
     */
    public String getCanonicalPath();

    /**
     * Get the type of this node.
     */
    public Type getType();

    /**
     * Test whether or not the node has children.  This is particularly useful for a node of Type.ROOT or Tyoe.LINK, neither
     * of which is a BRANCH nor a LEAF.
     *
     * @throws NoSuchElementException if this is a node of Type.LINK, but the destination doesn't exist.
     */
    public boolean hasChildren() throws NoSuchElementException;

    /**
     * The types of nodes.
     */
    public enum Type {
	ROOT, BRANCH, LEAF, LINK, UNRESOLVED;
    }
}
