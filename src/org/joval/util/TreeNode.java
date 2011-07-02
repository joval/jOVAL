// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A class that represents a tree structure and its nodes, with String representations using a specified delimiter.
 * Used by the CachingFilesystem base class to keep track of previously-visited files.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TreeNode {
    public static final int TYPE_BRANCH	= 0;
    public static final int TYPE_LEAF	= 1;

    private TreeNode parent;
    private String name, delimiter;
    List <TreeNode>children;
    int type;

    public static TreeNode makeRoot(String name, String delimiter) {
	return new TreeNode(name, delimiter);
    }

    /**
     * If parent is a leaf, invoking this method converts it to a branch.
     */
    public static TreeNode makeNode(TreeNode parent, String name) {
	TreeNode node = new TreeNode(parent, name);
	if (parent.isLeaf()) {
	    parent.type = TYPE_BRANCH;
	    parent.children = new Vector <TreeNode>();
	}
	parent.children.add(node);
	return node;
    }

    public String getName() {
	return name;
    }

    public boolean isLeaf() {
	return type == TYPE_LEAF;
    }

    public boolean isBranch() {
	return type == TYPE_BRANCH;
    }

    public int numChildren() {
	if (isLeaf()) {
	    return -1;
	} else {
	    return children.size();
	}
    }

    public TreeNode getChild(String name) throws NoSuchElementException {
	for (TreeNode node : children) {
	    if (name.equals(node.getName())) {
		return node;
	    }
	}
	throw new NoSuchElementException(name);
    }

    public String[] list() {
	int len = children.size();
	String[] sa = new String[children.size()];
	Iterator <TreeNode>iter = children.iterator();
	for (int i=0; iter.hasNext(); i++) {
	    sa[i] = iter.next().getName();
	}
	return sa;
    }

    private String path = null;
    public String toString() {
	if (parent == null) {
	    return name;
	} else if (path == null) {
	    path = new StringBuffer(parent.toString()).append(delimiter).append(name).toString();
	}
	return path;
    }

    /**
     * Find a node in the tree given its full path.
     */
    public TreeNode lookup(String path) throws NoSuchElementException {
	if (parent == null) {
	    StringTokenizer tok = new StringTokenizer(path, delimiter);
	    if (tok.countTokens() == 0) {
		throw new NoSuchElementException(path);
	    }
	    String rootName = tok.nextToken();
	    if (name.equals(rootName)) {
		TreeNode next = this;
		while (tok.hasMoreTokens()) {
		    String token = tok.nextToken();
		    next = next.getChild(token);
		}
		return next;
	    } else {
		throw new NoSuchElementException(path);
	    }
	} else {
	    return parent.lookup(path);
	}
    }

    // Private

    private TreeNode(TreeNode parent, String name) {
	this.parent = parent;
	delimiter = parent.delimiter;
	this.name = name;
	type = TYPE_LEAF;
    }

    /**
     * Create a root node.
     */
    private TreeNode(String name, String delimiter) {
	this.name = name;
	this.delimiter = delimiter;
	this.type = TYPE_LEAF;
    }
}
