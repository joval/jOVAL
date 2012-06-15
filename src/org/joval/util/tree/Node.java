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
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * A class that represents a node within a tree.
 *
 * Note that whenever a node is modified, it must be re-added to the Tree.nodes map, so that the changes will be
 * persisted in the JDBM storage.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Node implements INode {
    public static final int MAX_DEPTH = 50;

    Tree tree;
    Type type;
    boolean isLinkChild = false;
    String name = null, path = null, canonPath = null, linkPath = null, parentPath = null;
    HashSet<String> children = null;

    public void setBranch() {
	if (type != Type.TREE) {
	    type = Type.BRANCH;
	}
	if (children == null) {
	    children = new HashSet<String>();
	}
	tree.nodes.put(getPath(), this);
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
	if (children.contains(name)) {
	    String childPath = getPath() + tree.getDelimiter() + name;
	    if (tree.nodes.containsKey(childPath)) {
		return tree.nodes.get(childPath);
	    } else {
		throw new NoSuchElementException(childPath);
	    }
	} else {
	    throw new NoSuchElementException(name);
	}
    }

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	switch(type) {
	  case LINK:
	    if (hasChildren() && children == null) {
		copyChildren((Node)lookup(linkPath));
		tree.nodes.put(getPath(), this);
	    }
	    if (children == null) {
		throw new UnsupportedOperationException(JOVALMsg.getMessage(JOVALMsg.ERROR_NODE_CHILDREN, getPath(), type));
	    } else {
		Collection<INode> clist = new Vector<INode>();
		for (String childName : children) {
		    clist.add(getChild(childName));
		}
		return clist;
	    }

	  case UNRESOLVED: {
	    Node prototype = (Node)lookup(getCanonicalPath());
	    switch(prototype.type) {
	      case LINK:
		linkPath = prototype.linkPath;
		// fall-thru
	      default:
		type = prototype.type;
		break;
	    }
	    if (prototype.hasChildren()) {
		copyChildren(prototype);
	    }
	    tree.nodes.put(getPath(), this);
	    return getChildren();
	  }

	  case LEAF:
	    throw new UnsupportedOperationException(JOVALMsg.getMessage(JOVALMsg.ERROR_NODE_CHILDREN, getPath(), type));

	  case BRANCH: {
	    Collection<INode> clist = new Vector<INode>();
	    for (String childName : children) {
		clist.add(getChild(childName));
	    }
	    return clist;
	  }

	  case TREE:
	  default:
	    if (children == null) {
		throw new NoSuchElementException(getPath());
	    } else {
		Collection<INode> clist = new Vector<INode>();
		for (String childName : children) {
		    clist.add(getChild(childName));
		}
		return clist;
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
	if (path == null) {
	    if (parentPath == null) {
		if (name.endsWith(tree.getDelimiter())) {
		    path = name.substring(0, name.lastIndexOf(tree.getDelimiter()));
		} else {
		    path = name;
		}
	    } else {
		path = new StringBuffer(parentPath).append(tree.getDelimiter()).append(name).toString();
	    }
	    tree.nodes.put(path, this);
	}
	return path;
    }

    public String getCanonicalPath() {
	if (canonPath == null) {
	    if (parentPath == null) {
		canonPath = name;
	    } else {
		String parentCanon = null;
		if (isLinkChild) {
		    String realPath = tree.links.get(getPath());
		    canonPath = tree.lookup(realPath).getCanonicalPath();
		} else {
		    Node parent = (Node)tree.lookup(parentPath);
		    parentCanon = parent.getCanonicalPath();
		    if (parentCanon.endsWith(tree.getDelimiter())) {
			canonPath = new StringBuffer(parentCanon).append(name).toString();
		    } else {
			canonPath = new StringBuffer(parentCanon).append(tree.getDelimiter()).append(name).toString();
		    }
		}
	    }
	    tree.nodes.put(path, this);
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
	parentPath = parent.getPath();
	tree = parent.tree;
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
	    getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_NODE_LINK, e.getMessage()));
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
	return tree.lookup(path, false);
    }

    /**
     * Copy children from a source (i.e., the canonical node).
     */
    private void copyChildren(Node source) {
	Collection<INode> adoptees = source.getChildren();
	if (adoptees.size() == 0) {
	    //
	    // Link to an empty directory
	    //
	    children = new HashSet<String>();
	} else {
	    for (INode inode : adoptees) {
		Node child = (Node)inode;
		if (getCanonicalPath().equals(child.getCanonicalPath())) {
		    tree.getLogger().error(JOVALMsg.ERROR_LINK_SELF, getCanonicalPath());
		} else {
		    Node myChild = tree.makeNode(this, child.getName());
		    myChild.type = Type.UNRESOLVED;
		    if (type == Type.LINK) {
			myChild.isLinkChild = true;
			tree.links.put(myChild.getPath(), child.getPath());
		    }
		}
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
