// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Generic implementation of a forest.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Forest implements IForest, Iterable<ITree> {
    private String name, delimiter;
    private Hashtable<String, ITree> trees;
    private LocLogger logger;

    public Forest(String name, String delimiter) {
	this.name = name;
	this.delimiter = delimiter;
	trees = new Hashtable<String, ITree>();
	logger = JOVALSystem.getLogger();
    }

    public Tree makeTree(String name) {
	Tree tree = new Tree(this, name);
	addTree(tree);
	return tree;
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement INode

    public ITree getTree() {
	return null;
    }

    public String getName() {
	return name;
    }

    public String getPath() {
	return "forest://" + name;
    }

    public String getCanonicalPath() {
	return getPath();
    }

    public Type getType() {
	return Type.FOREST;
    }

    public INode getChild(String name) throws NoSuchElementException {
	return getTree(name);
    }

    public Collection<INode> getChildren() {
	return getChildren(null);
    }

    public Collection<INode> getChildren(Pattern p) {
	Collection<INode> children = new Vector<INode>();
	for (ITree tree : this) {
	    if (p == null || p.matcher(tree.getName()).find()) {
		children.add(tree);
	    }
	}
	return children;
    }

    public boolean hasChildren() {
	return trees.size() > 0;
    }

    // Implement ITree

    public int size() {
	int count = 0;
	for (ITree tree : this) {
	    count += tree.size();
	    count++; // include the tree itself as a node
	}
	return count;
    }

    public Collection<String> search(Pattern p, boolean followLinks) {
	Collection<String> result = new Vector<String>();
	for (ITree tree : this) {
	    result.addAll(tree.search(p, followLinks));
	}
	return result;
    }

    public INode lookup(String path) {
	INode result = null;
	for (ITree tree : this) {
	    try {
		return ((Tree)tree).lookup(path);
	    } catch (NoSuchElementException e) {
	    }
	}
	throw new NoSuchElementException(path);
    }

    // Implement IForest

    public String getDelimiter() {
	return delimiter;
    }

    public ITree addTree(ITree tree) throws IllegalArgumentException {
	for (ITree t : this) {
	    //
	    // Make sure none of the existing trees have this tree as a path
	    //
	    try {
		Node node = (Node)t.lookup(tree.getName());
		switch(node.getType()) {
		  case TREE:
		    // ignore - the target is being replaced
		    break;
		  case LEAF:
		    ((Tree)node.getTree()).remove(node);
		    break;
		  default:
		    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_TREE_ADD, tree.getName(), node.getType(), t.getName());
		    throw new IllegalArgumentException(msg);
		}
	    } catch (NoSuchElementException e) {
		// no problem
	    }

	    //
	    // Make sure this tree has none of the existing trees as a path
	    //
	    try {
		Node node = (Node)tree.lookup(t.getName());
		switch(node.getType()) {
		  case TREE:
		    // ignore - the target is being replaced
		    break;
		  case LEAF:
		    ((Tree)node.getTree()).remove(node);
		    break;
		  default:
		    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_TREE_ADD, tree.getName(), node.getType(), t.getName());
		    throw new IllegalArgumentException(msg);
		}
	    } catch (NoSuchElementException e) {
		// no problem
	    }
	}

	return trees.put(tree.getName(), tree);
    }

    public ITree getTree(String name) throws NoSuchElementException {
	if (trees.containsKey(name)) {
	    return trees.get(name);
	} else {
	    throw new NoSuchElementException(name);
	}
    }

    // Implement Iterable

    public Iterator<ITree> iterator() {
	return trees.values().iterator();
    }
}
