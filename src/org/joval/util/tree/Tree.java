// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

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
    LocLogger logger;

    public Tree(String name, String delimiter) {
	this.delimiter = delimiter;
	root = new Node(this, name);
	logger = JOVALSystem.getLogger();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
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
	logger.trace(JOVALMsg.STATUS_TREE_MKNODE, root.getName(), node.getPath());
	return node;
    }

    public String makeLink(INode node, String destination) throws UnsupportedOperationException {
	String err = null;
	if (node instanceof Node) {
	    Node n = (Node)node;
	    switch(n.type) {
	      case LEAF:
	      case LINK: // over-link?
        	n.type = INode.Type.LINK;
        	n.linkPath = resolvePath(n.parent == null ? "" : n.parent.getPath(), destination);
		logger.trace(JOVALMsg.STATUS_TREE_MKLINK, n.getPath(), destination);
		return n.linkPath;

	      default:
        	err = JOVALSystem.getMessage(JOVALMsg.ERROR_TREE_MKLINK, n.getPath(), destination, n.type);
		break;
	    }
        } else {
	    err = JOVALSystem.getMessage(JOVALMsg.ERROR_TREE_NODE, node.getClass().getName());
	}
        throw new UnsupportedOperationException(err);
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

    // Private

    /**
     * Resolve an absolute path from a relative path from a base file path.
     */
    private String resolvePath(String origin, String rel) throws UnsupportedOperationException {
	if (rel.startsWith(delimiter)) {
	    return rel;
	} else {
	    Stack<String> stack = new Stack<String>();
	    for (String s : StringTools.toList(StringTools.tokenize(origin, delimiter))) {
		stack.push(s);
	    }
	    for (String next : StringTools.toList(StringTools.tokenize(rel, delimiter))) {
		if (next.equals(".")) {
		    // stay in the same place
		} else if (next.equals("..")) {
		    if (stack.empty()) {
			// links above root stay at root
		    } else {
			stack.pop();
		    }
		} else {
		    stack.push(next);
		}
	    }
	    StringBuffer path = new StringBuffer();
	    while(!stack.empty()) {
		StringBuffer elt = new StringBuffer(delimiter);
		path.insert(0, elt.append(stack.pop()).toString());
	    }
	    if (path.length() == 0) {
		return delimiter;
	    } else {
		return path.toString();
	    }
	}
    }
}
