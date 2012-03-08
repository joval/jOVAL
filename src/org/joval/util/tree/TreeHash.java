// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Hashtable;

import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.INode;
import org.joval.util.StringTools;

/**
 * A class that represents a forest of tree structures with objects at its nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TreeHash<T> {
    private Forest forest;
    private Hashtable<String, T> table;

    public TreeHash(String name, String delimiter) {
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
	// The path was not found, so get the name of the Tree (root node) for the path.
	//
	List<String> tokens = StringTools.toList(StringTools.tokenize(path, forest.getDelimiter(), false));
	String rootName = tokens.get(0);
	if (rootName.length() == 0) {
	    //
	    // This is the special case of the root node whose name is the delimiter
	    //
	    rootName = forest.getDelimiter();
	}

	Tree root = null;
	try {
	    root = (Tree)forest.getTree(rootName);
	} catch (NoSuchElementException e) {
	    root = forest.makeTree(rootName);
	}

	INode node = root;
	for (int i=1; i < tokens.size(); i++) {
	    String token = tokens.get(i);
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
