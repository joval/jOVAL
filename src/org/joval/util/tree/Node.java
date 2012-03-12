// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
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
    public static final int MAX_DEPTH = 50;

    Tree tree;
    Type type;
    String name = null, path = null, canonPath = null, linkPath = null;
    Node parent = null, linkParent = null;
    Hashtable<String, INode> children;

    public void setBranch() {
	type = Type.BRANCH;
	if (children == null) {
	    children = new Hashtable<String, INode>();
	}
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return tree.getLogger();
    }

    public void setLogger(LocLogger logger) {
	tree.setLogger(logger);
    }

    // Implement INode

    public ITree getTree() {
	return tree;
    }

    public String getName() {
	return name;
    }

    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException {
	if (children == null) {
	    getChildren();
	}
	if (children.containsKey(name)) {
	    return children.get(name);
	} else {
	    throw new NoSuchElementException(name);
	}
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
		return children.values();
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
	    return children.values();

	  case TREE:
	  default:
	    if (children == null) {
		throw new NoSuchElementException(getPath());
	    } else {
		return children.values();
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

	  case TREE:
	  case BRANCH:
	  default:
	    return children != null;
	}
    }

    public String getPath() {
	if (parent == null) {
	    if (name.endsWith(tree.getDelimiter())) {
		path = name.substring(0, name.lastIndexOf(tree.getDelimiter()));
	    } else {
		path = name;
	    }
	} else if (path == null) {
	    if (linkParent == null) {
		path = new StringBuffer(parent.getPath()).append(tree.getDelimiter()).append(name).toString();
	    } else {
		path = new StringBuffer(linkParent.getPath()).append(tree.getDelimiter()).append(name).toString();
	    }
	}
	return path;
    }

    public String getCanonicalPath() {
	if (parent == null) {
	    return name;
	} else if (canonPath == null) {
	    String parentCanon = parent.getCanonicalPath();
	    if (parentCanon.endsWith(tree.getDelimiter())) {
		canonPath = new StringBuffer(parentCanon).append(name).toString();
	    } else {
		canonPath = new StringBuffer(parentCanon).append(tree.getDelimiter()).append(name).toString();
	    }
	}
	return canonPath;
    }

    public Type getType() {
	return type;
    }

    // Internal

    Node(String name) {
	this.name = name;
    }

    /**
     * Constructor for a regular node.
     */
    Node(Node parent, String name) {
	this(name);
	this.parent = parent;
	this.tree = parent.tree;
	type = Type.LEAF;
    }

    /**
     * Recursive search for matching pathnames.
     */
    Collection<String> search(Pattern p, boolean followLinks, Stack<Node> visited) {
	Collection<String> result = new Vector<String>();
	if (p.matcher(getPath()).find()) {
	    result.add(getPath());
	}

	try {
	    if (searchable(followLinks, visited)) {
		visited.push(this);
		for (INode child : getChildren()) {
		    result.addAll(((Node)child).search(p, followLinks, visited));
		}
		visited.pop();
	    }
	} catch (NoSuchElementException e) {
	    getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_NODE_LINK, e.getMessage()));
	} catch (MaxDepthException e) {
	    getLogger().warn(JOVALMsg.ERROR_NODE_DEPTH, e.getMessage());
	}
	return result;
    }

    // Private

    /**
     * Convenience method for looking up another node in the Forest (yes, FOREST).
     */
    private INode lookup(String path) throws NoSuchElementException {
	if (parent instanceof ITree) {
	    return ((Tree)parent).lookup(path, false);
	} else {
	    return parent.lookup(path);
	}
    }

    /**
     * Copy from canonical into a link path.
     */
    private void copyChildren(Node target) {
	children = new Hashtable<String, INode>();
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
		children.put(child.getName(), child);
	    }
	}
    }

    /**
     * Determines whether this node can be searched (i.e., depth < max, and not already visited for the search).
     */
    private boolean searchable(boolean followLinks, Stack<Node> visited) throws MaxDepthException, NoSuchElementException {
	if (hasChildren() && (followLinks || type != Type.LINK)) {
	    if (visited.size() > MAX_DEPTH) {
		throw new MaxDepthException(getPath());
	    }

	    String canon = getCanonicalPath();
	    for (Node node : visited) {
		if (canon.equals(node.getCanonicalPath())) {
		    tree.getLogger().warn(JOVALMsg.ERROR_LINK_CYCLE, getPath(), node.getPath());
		    return false;
		}
	    }
	    return true;
	} else {
	    return false;
	}
    }

    private class MaxDepthException extends Exception {
	private MaxDepthException(String message) {
	    super(message);
	}
    }
}
