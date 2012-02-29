// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALSystem;

/**
 * Utility class for an IProducer.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Forest implements IForest {
    Hashtable<String, ITree> trees;
    LocLogger logger;

    public Forest() {
	trees = new Hashtable<String, ITree>();
	logger = JOVALSystem.getLogger();
    }

    public ITree addTree(ITree tree) {
	return addTree(tree, tree.getRoot().getName());
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
	for (ITree tree : getTrees()) {
	    tree.setLogger(logger);
	}
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement IForest

    public ITree addTree(ITree tree, String name) {
	tree.setLogger(logger);
	return trees.put(name, tree);
    }

    public ITreeBuilder getTreeBuilder(String name) throws SecurityException {
	ITree tree = trees.get(name);
	if (tree == null) {
	    return null;
	} else if (tree instanceof ITreeBuilder) {
	    return (ITreeBuilder)tree;
	}
	throw new SecurityException(name);
    }

    public ITree getTree(String name) {
	return trees.get(name);
    }

    public Collection<ITree> getTrees() {
	return trees.values();
    }

    public Collection<String> search(Pattern p) {
	Collection<String> result = new Vector<String>();
	for (ITree tree : getTrees()) {
	    result.addAll(tree.search(p));
	}
	return result;
    }

    public INode lookup(String path) {
	INode result = null;
	for (ITree tree : getTrees()) {
	    try {
		if (tree.getRoot().getName().equals(path)) {
		    return tree.getRoot();
		} else {
		    return tree.lookup(path);
		}
	    } catch (NoSuchElementException e) {
	    }
	}
	throw new NoSuchElementException(path);
    }
}
