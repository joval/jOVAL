// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.util.tree.INode;
import org.joval.util.StringTools;

/**
 * A class that represents a tree structure with objects at its nodes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TreeHash extends Tree {
    public TreeHash(String name, String delimiter) {
	super(name, delimiter);
	root = new DataNode(this, name);
    }

    /**
     * Get a hierarchical property value.
     *
     * @param path denotes path beneath the root node.
     */
    public Object getData(String path) throws NoSuchElementException {
	return ((DataNode)lookup(path)).getData();
    }

    /**
     * Set data at a path.
     *
     * @param path denotes path beneath the root node.
     */
    public void putData(String path, Object data) {
	((DataNode)getCreateNode(path)).setData(data);
    }

    // Overrides

    public INode makeNode(INode parent, String name) {
	Node p = (Node)parent;
	DataNode node = new DataNode(p, name);
	if (p.getType() == INode.Type.LEAF) {
	    p.type = INode.Type.BRANCH;
	}
	if (p.children == null) {
	    p.children = new Vector<INode>();
	}
	p.children.add(node);
	return node;
    }

    public INode makeLink(INode parent, String name, String destination) {
	throw new UnsupportedOperationException("Links don't make sense in a TreeHash");
    }

    // Private

    class DataNode extends Node {
	private Object data;

	DataNode(Node parent, String name) {
	    super(parent, name);
	}

	DataNode(Tree tree, String name) {
	    super(tree, name);
	}

	/**
	 * Look at ancestor properties until a value is found.
	 */
	Object getData() {
	    return data;
	}

	void setData(Object data) {
	    this.data = data;
	}
    }

    /**
     * Get the node at the path if it exists, if not, make it and all the nodes necessary, then return it.
     *
     * @param path denotes a path beneath the root node.
     */
    private INode getCreateNode(String path) {
	Collection<String> tokens = StringTools.toList(StringTools.tokenize(path, delimiter));
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
		node = makeNode(node, token);
	    }
	}
	return node;
    }
}
