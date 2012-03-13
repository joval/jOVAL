// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.Vector;

import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
import org.joval.util.StringTools;

/**
 * A class that represents a forest of tree structures with objects at its nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TreeHash<T> {
    private String delim;
    private Forest forest;
    private Hashtable<String, T> table;

    public TreeHash(String name, String delimiter) {
	delim = delimiter;
	forest = new Forest(name, delimiter);
	table = new Hashtable<String, T>();
    }

    public Forest getRoot() {
	return forest;
    }

    /**
     * Get data from a node.
     *
     * @param path denotes path beneath the root node.
     */
    public T getData(String path) throws NoSuchElementException {
	return table.get(forest.lookup(path).getCanonicalPath());
    }

    /**
     * Put data at a node. The path should always be a canonical path. Data can be removed by passing in a null value
     * for data.
     *
     * @returns the INode where the data was stored.
     */
    public INode putData(String path, T data) {
	if (data == null) {
	    if (table.containsKey(path)) {
		table.remove(path);
	    }
	    try {
		return forest.lookup(path);
	    } catch (NoSuchElementException e) {
		return null;
	    }
	} else {
	    INode node = getCreateNode(path);
	    table.put(path, data);
	    return node;
	}
    }

    /**
     * Get the node at the path if it exists, if not, make it and all the nodes necessary, then return it.
     * If the path cannot be found but begins with the delimiter, it is assumed that there is a "root" tree whose
     * name is the delimiter.
     */
    public INode getCreateNode(String path) {
	try {
	    return forest.lookup(path);
	} catch (NoSuchElementException e) {
	}

	//
	// The path was not found, so compare it to the names of existing trees in the forest. If any of the tree names
	// match the beginning of the path, then the root name is name of the tree with the longest matching name.
	//
	// If no existing trees match, then the root name is the token up to the first delimiter, or the delimiter itself.
	//
	int matchLen = 0;
	String rootName = null;
	for (ITree tree : forest) {
	    if (tree.getName().equals(path)) {
		return tree;
	    } else {
		String prefix = null;
		if (tree.getName().endsWith(delim)) {
		    prefix = tree.getName();
		} else {
		    prefix = tree.getName() + delim;
		}
		if (path.startsWith(prefix)) {
		    if (prefix.length() > matchLen) {
			rootName = tree.getName();
			matchLen = prefix.length();
		    }
		}
	    }
	}
	List<String> tokens = null;
	if (rootName == null) {
	    int ptr = path.indexOf(delim);
	    if (ptr > 0) {
		rootName = path.substring(0, ptr);
		tokens = StringTools.toList(StringTools.tokenize(path.substring(ptr), delim));
	    } else if (ptr == 0) {
		//
		// This is the special case of the root node whose name is the delimiter
		//
		rootName = delim;
		tokens = StringTools.toList(StringTools.tokenize(path.substring(delim.length()), delim));
	    } else {
		rootName = path;
		tokens = new Vector<String>();
	    }
	} else {
	    tokens = StringTools.toList(StringTools.tokenize(path.substring(matchLen), delim));
	}

	Tree root = null;
	try {
	    root = (Tree)forest.getTree(rootName);
	} catch (NoSuchElementException e) {
	    root = forest.makeTree(rootName);
	}

	if (rootName.equals(path)) {
	    return root;
	} else {
	    INode node = root;
	    for (String token : tokens) {
		boolean makeNode = false;
		try {
		    node = node.getChild(token);
		} catch (UnsupportedOperationException e) {
		    makeNode = true;
		} catch (NoSuchElementException e) {
		    makeNode = true;
		}
		if (makeNode) {
		    node = root.makeNode((Node)node, token);
		}
	    }
	    return node;
	}
    }
}
