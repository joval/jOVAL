// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.util.tree.INode;

/**
 * A class that represents a tree structure and its nodes, with String representations using a specified delimiter.
 * Used by the CachingTree base class to keep track of previously-visited nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Tree implements ITreeBuilder {
    String delimiter;
    Node root;

    public Tree(String name, String delimiter) {
	this.delimiter = delimiter;
	root = new Node(this, name);
    }

    // Implement ITreeBuilder

    public INode makeNode(INode parent, String name) {
	Node p = (Node)parent;
	Node node = new Node(p, name);
	if (p.getType() == INode.Type.LEAF) {
	    p.type = INode.Type.BRANCH;
	}
	if (p.children == null) {
	    p.children = new Vector<INode>();
	}
	p.children.add(node);
	return node;
    }

    public INode makeLink(INode parent, String name, String destination) {
	Node node = (Node)makeNode(parent, name);
	node.type = INode.Type.LINK;
	node.linkPath = destination;
	return node;
    }

    // Implement ITree

    public INode lookup(String path) throws NoSuchElementException {
	return root.lookup(path);
    }

    public String getDelimiter() {
	return delimiter;
    }

    public Collection<String> search(Pattern p, boolean followLinks) {
	return root.search(p, followLinks);
    }

    public INode getRoot() {
	return root;
    }
}
