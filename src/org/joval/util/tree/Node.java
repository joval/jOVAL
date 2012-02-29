// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.util.tree.INode;
import org.joval.util.StringTools;

/**
 * A class that represents a node within a tree.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Node implements INode, Cloneable {
    Tree tree;
    Node parent, linkParent = null;
    String name;
    Collection<INode> children;
    Type type;
    String path = null, canonPath = null;
    String linkPath = null;

    // Implement INode

    public String getName() {
	return name;
    }

    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException {
	switch(type) {
	  case LINK: {
	    try {
		Node child = (Node)((Node)lookup(linkPath).getChild(name)).clone();
		child.linkParent = this;
		return child;
	    } catch (CloneNotSupportedException e) {
		throw new RuntimeException(e);
	    }
	  }

	  case LEAF:
	    throw new UnsupportedOperationException(name);

	  case ROOT:
	  case BRANCH:
	  default:
	    for (INode node : getChildren()) {
		if (name.equals(node.getName())) {
		    return node;
		}
	    }
	    throw new NoSuchElementException(name);
	}
    }

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	switch(type) {
	  case LINK:
	    if (hasChildren() && children == null) {
		try {
		    children = new Vector<INode>();
		    for (INode node : lookup(linkPath).getChildren()) {
			Node child = (Node)((Node)node).clone();
			child.linkParent = this;
			children.add(child);
		    }
		    return children;
		} catch (CloneNotSupportedException e) {
		    throw new RuntimeException(e);
		}
	    } else {
		return children;
	    }

	  case LEAF:
	    throw new UnsupportedOperationException("getChildren");

	  case BRANCH:
	    if (linkParent == null) {
		return children;
	    } else {
		try {
		    Collection<INode> result = new Vector<INode>();
		    for (INode node : children) {
			Node child = (Node)((Node)node).clone();
			child.linkParent = this;
		    }
		    return result;
		} catch (CloneNotSupportedException e) {
		    throw new RuntimeException(e);
		}
	    }

	  case ROOT:
	  default:
	    if (children == null) {
		throw new NoSuchElementException(getPath());
	    } else {
		return children;
	    }
	}
    }

    public Collection<INode> getChildren(Pattern p) throws NoSuchElementException, UnsupportedOperationException {
	Collection<INode> result = new Vector<INode>();
	for (INode node : getChildren()) {
	    if (p.matcher(node.getName()).find()) {
		result.add(node);
	    }
	}
	return result;
    }

    public boolean hasChildren() throws NoSuchElementException {
	switch(type) {
	  case LINK:
	    return lookup(linkPath).hasChildren();

	  case LEAF:
	    return false;

	  case ROOT:
	  case BRANCH:
	  default:
	    return children != null;
	}
    }

    public String getPath() {
	if (parent == null) {
	    return name;
	} else if (path == null) {
	    if (linkParent == null) {
		path = new StringBuffer(parent.getPath()).append(tree.delimiter).append(name).toString();
	    } else {
		path = new StringBuffer(linkParent.getPath()).append(tree.delimiter).append(name).toString();
	    }
	}
	return path;
    }

    public String getCanonicalPath() {
	if (parent == null) {
	    return name;
	} else if (canonPath == null) {
	    canonPath = new StringBuffer(parent.getCanonicalPath()).append(tree.delimiter).append(name).toString();
	}
	return canonPath;
    }

    public Type getType() {
	return type;
    }

    // Internal

    Collection<String> search(Pattern p) {
	Collection<String> result = new Vector<String>();
	if (p.matcher(getPath()).find()) {
	    result.add(getPath());
	}
	if (hasChildren()) {
	    for (INode child : getChildren()) {
		result.addAll(((Node)child).search(p));
	    }
	}
	return result;
    }

    INode lookup(String path) throws NoSuchElementException {
	if (parent == null) {
	    Iterator<String> iter = StringTools.tokenize(path, tree.delimiter, false);
	    if (!iter.hasNext()) {
		throw new NoSuchElementException(path);
	    }
	    String rootName = iter.next();
	    if (name.equals(rootName)) {
		Node next = this;
		while (iter.hasNext()) {
		    String token = iter.next();
		    next = (Node)next.getChild(token);
		}
		return next;
	    } else {
		throw new NoSuchElementException(path);
	    }
	} else {
	    return parent.lookup(path);
	}
    }

    Node(Node parent, String name) {
	this.parent = parent;
	this.tree = parent.tree;
	this.name = name;
	type = Type.LEAF;
    }

    /**
     * Create a root node.
     */
    Node(Tree tree, String name) {
	this.name = name;
	this.tree = tree;
	this.type = Type.ROOT;
    }
}
