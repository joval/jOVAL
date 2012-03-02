// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALSystem;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * A class that represents a node within a tree.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Node implements INode {
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
	for (INode node : getChildren()) {
	    if (name.equals(node.getName())) {
		return node;
	    }
	}
	throw new NoSuchElementException(name);
    }

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	switch(type) {
	  case LINK:
	    if (hasChildren() && children == null) {
		copyChildren((Node)lookup(linkPath));
	    }
	    if (children == null) {
		throw new UnsupportedOperationException(JOVALSystem.getMessage(JOVALMsg.ERROR_NODE_CHILDREN, getPath(), type));
	    } else {
		return children;
	    }

	  case UNRESOLVED: {
	    Node prototype = (Node)lookup(getCanonicalPath());
	    if (prototype.hasChildren()) {
		copyChildren(prototype);
	    }
	    linkPath = prototype.linkPath; // may or may not be a link
	    type = prototype.type;
	    return getChildren();
	  }

	  case LEAF:
	    throw new UnsupportedOperationException(JOVALSystem.getMessage(JOVALMsg.ERROR_NODE_CHILDREN, getPath(), type));

	  case BRANCH:
	    return children;

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

	  case UNRESOLVED:
	    return lookup(getCanonicalPath()).hasChildren();

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

    Collection<String> search(Pattern p, boolean followLinks) {
	Collection<String> result = new Vector<String>();
	if (p.matcher(getPath()).find()) {
	    result.add(getPath());
	}

	try {
	    if (searchable(followLinks)) {
		try {
		    if (hasChildren()) {
			for (INode child : getChildren()) {
			    result.addAll(((Node)child).search(p, followLinks));
			}
		    }
		} catch (NoSuchElementException e) {
		    tree.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_NODE_LINK, e.getMessage()));
		}
	    }
	} catch (MaxDepthException e) {
	    tree.getLogger().warn(JOVALMsg.ERROR_NODE_DEPTH, e.getMessage());
	}
	return result;
    }

    INode lookup(String path) throws NoSuchElementException {
	if (parent == null) {
if("/usr/lib/nls/msg/de_DE.ISO8859-1/ifconfig.ib.cat".equals(path))System.out.println("WOOOOOOOHOOOOOOOOOO!!!!!");
	    Iterator<String> iter = StringTools.tokenize(path, tree.delimiter, false);
	    if (!iter.hasNext()) {
		throw new NoSuchElementException(path);
	    }
	    String rootName = iter.next();
	    if (name.equals(rootName)) {
		Node next = this;
		while (iter.hasNext()) {
		    String token = iter.next();
		    if (token.length() > 0) {
			try {
			    next = (Node)next.getChild(token);
			} catch (UnsupportedOperationException e) {
			    throw new NoSuchElementException(path);
			}
		    }
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

    // Private

    private static final int MAX_DEPTH = 50;

    private boolean searchable(boolean followLinks) throws MaxDepthException {
	int depth = StringTools.toList(StringTools.tokenize(getPath(), tree.delimiter)).size();
	if (depth > MAX_DEPTH) {
	    throw new MaxDepthException(getPath());
	}
	return followLinks || type != Type.LINK;
    }

    private class MaxDepthException extends Exception {
	private MaxDepthException(String message) {
	    super(message);
	}
    }

    /**
     * Copy from canonical into a link path.
     */
    private void copyChildren(Node target) {
	children = new Vector<INode>();
	for (INode inode : target.getChildren()) {
	    Node node = (Node)inode;
	    if (getCanonicalPath().equals(node.getCanonicalPath())) {
		tree.getLogger().error(JOVALMsg.ERROR_LINK_SELF, getCanonicalPath());
	    } else {
		Node child = new Node(node.parent, node.getName());
		child.linkParent = this;
		switch(((Node)node).type) {
		  case LEAF:
		    child.type = Type.LEAF;
		    break;
		  default:
		    child.type = Type.UNRESOLVED;
		    break;
		}
		children.add(child);
	    }
	}
    }
}
