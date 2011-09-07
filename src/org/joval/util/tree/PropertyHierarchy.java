// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.util.tree.INode;
import org.joval.util.StringTools;

/**
 * A class that represents a tree structure and property sets at its nodes.  Getting a property of a Node will cause a search
 * towards the root until the named property is found.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PropertyHierarchy extends Tree {
    public PropertyHierarchy(String name, String delimiter) {
	super(name, delimiter);
	root = new PropertyNode(this, name);
    }

    /**
     * Get a hierarchical property value.
     *
     * @param path denotes path beneath the root node.
     */
    public String getProperty(String path, String key) {
	return ((PropertyNode)nearestAncestor(path)).getProperty(key);
    }

    /**
     * Set a hierarchical property value.
     *
     * @param path denotes path beneath the root node.
     */
    public void setProperty(String path, String key, String value) {
	((PropertyNode)getCreateNode(path)).setProperty(key, value);
    }

    // Overrides

    public INode makeNode(INode parent, String name) {
	Node p = (Node)parent;
	PropertyNode node = new PropertyNode(p, name);
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
	throw new UnsupportedOperationException("Links don't make sense in a PropertyHierarchy");
    }

    class PropertyNode extends Node {
	private Properties props;

	PropertyNode(Node parent, String name) {
	    super(parent, name);
	    props = new Properties();
	}

	PropertyNode(Tree tree, String name) {
	    super(tree, name);
	    props = new Properties();
	}

	/**
	 * Look at ancestor properties until a value is found.
	 */
	String getProperty(String key) {
	    if (props.containsKey(key)) {
		return props.getProperty(key);
	    } else if (parent == null) {
		return null;
	    } else {
		return ((PropertyNode)parent).getProperty(key);
	    }
	}

	void setProperty(String key, String value) {
	    props.setProperty(key, value);
	}
    }

    /**
     * @param path denotes path beneath the root node.
     */
    private INode nearestAncestor(String path) {
	Collection<String> tokens = StringTools.toList(StringTools.tokenize(path, delimiter));
	INode ancestor = root;
	try {
	    for (String token : tokens) {
		ancestor = ancestor.getChild(token);
	    }
	} catch (UnsupportedOperationException e) {
	} catch (NoSuchElementException e) {
	}
	return ancestor;
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
