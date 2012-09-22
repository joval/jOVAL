// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
 * A class that represents a tree structure and its nodes, with String representations using a specified delimiter.
 * Used by the CachingTree base class to keep track of previously-visited nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Tree extends Node implements ITree {
    private Forest forest;
    private int linkCount = 0;

    Map<String, Node> nodes;
    Map<String, String> links; // mapping of virtualPath -> realPath

    Tree(Forest forest, String name, Map<String, Node> nodes, Map<String, String> links) {
	super(name);
	tree = this;
	this.forest = forest;
	this.type = Type.TREE;
	this.nodes = nodes;
	this.links = links;
	nodes.put(getPath(), this);
    }

    /**
     * Make a new node in this tree.
     */
    public Node makeNode(Node parent, String name) {
	Node node = new Node(parent, name);
	if (parent.getType() == INode.Type.LEAF) {
	    parent.type = INode.Type.BRANCH;
	}
	if (parent.children == null) {
	    parent.children = new HashSet<String>();
	}
	parent.children.add(name);
	getLogger().trace(JOVALMsg.STATUS_TREE_MKNODE, name, node.getPath());
	nodes.put(parent.getPath(), parent); // re-commit the parent
	nodes.put(node.getPath(), node);
	return node;
    }

    /**
     * Convert a node into a link to the specified destination.
     */
    public void makeLink(Node node, String destination) throws UnsupportedOperationException {
	switch(node.type) {
	  case LINK: // repeat?
	    if (destination != null && destination.equals(node.linkPath)) {
		break;
	    }
	    // fall-thru
	  case UNRESOLVED:
	  case LEAF:
            node.type = INode.Type.LINK;
	    if (destination == null) {
        	node.linkPath = node.getPath(); // DAS: link to self?
	    } else {
        	node.linkPath = destination;
	    }
	    getLogger().debug(JOVALMsg.STATUS_TREE_MKLINK, node.getPath(), destination);
	    linkCount++;
	    break;

	  default:
            String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_TREE_MKLINK, node.getPath(), destination, node.type);
    	    throw new UnsupportedOperationException(msg);
	}
	nodes.put(node.getPath(), node);
    }

    // Implement ILoggable

    @Override
    public LocLogger getLogger() {
	return forest.getLogger();
    }

    @Override
    public void setLogger(LocLogger logger) {
	forest.setLogger(logger);
    }

    // Implement ITree

    public Collection<String> search(Pattern p, boolean followLinks) {
	if (linkCount == 0 || !followLinks) {
	    Collection<String> results = new Vector<String>();
	    for (String path : nodes.keySet()) {
		if (p.matcher(path).find()) {
		    results.add(path);
		}
	    }
	    return results;
	} else {
	    return super.search(p, followLinks, new Stack<Node>());
	}
    }

    public INode lookup(String path) throws NoSuchElementException {
	if (nodes.containsKey(path)) {
	    return nodes.get(path);
	} else {
	    return lookup(path, true);
	}
    }

    public int size() {
	return nodes.size();
    }

    // Internal

    void remove(Node node) {
	nodes.remove(node.getPath());
	nodes.get(node.parentPath).children.remove(node.getName());
	nodes.put(node.getPath(), node);
    }

    String getDelimiter() {
	return forest.getDelimiter();
    }

    /**
     * Internal implementation to look up a node.
     *
     * @param local set to true to remain inside this tree, set to false to expand the scope to the whole forest.
     */
    INode lookup(String path, boolean local) throws NoSuchElementException {
	if (path.equals(name)) {
	    return this;
	} else if (path.startsWith(rootPath())) {
	    String subpath = path.substring(rootPath().length());
	    Iterator<String> iter = StringTools.tokenize(subpath, forest.getDelimiter());
	    if (iter.hasNext()) {
		try {
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
		} catch (NoSuchElementException e) {
		    // get a chance for non-local lookup
		}
	    } else {
		// should be impossible to reach this point.
		throw new NoSuchElementException(subpath);
	    }
	}
	if (local) {
	    throw new NoSuchElementException(path);
	} else {
	    return forest.lookup(path);
	}
    }

    // Private

    /**
     * Get the delimiter-terminated pathname of the root node.
     */
    private String rootPath() {
	if (name.endsWith(forest.getDelimiter())) {
	    return name;
	} else {
	    return name + forest.getDelimiter();
	}
    }
}
