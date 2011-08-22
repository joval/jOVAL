// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
import org.joval.util.tree.Forest;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.tree.Tree;

/**
 * An abstract tree implementation that caches search results for better performance.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CachingTree implements ITree {
    protected IForest cache;
    private String ESCAPED_DELIM;

    public CachingTree() {
	cache = new Forest();
	ESCAPED_DELIM = Matcher.quoteReplacement(getDelimiter());
    }

    protected abstract boolean preload();

    // Implement ITree (sparsely) -- subclasses must implement only the getDelimiter and lookup methods.

    public INode makeNode(INode parent, String name) {
	return null;
    }

    public INode makeLink(INode parent, String name, String destinationPath) {
	return null;
    }

    public INode getRoot() {
	return null;
    }

    public Collection<String> search(Pattern p, boolean followLinks) {
	if (preload()) {
	    try {
		return cache.search(p, followLinks);
	    } catch (PatternSyntaxException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()), e);
	    }
	    return null;
	} else {
	    return smartSearch(p.pattern(), followLinks);
	}
    }

    // Internal

    /**
     * Get the first token from the given path.  The delimiter is the escaped result of getDelimiter().
     */
    protected final String getToken(String path) {
	int ptr = path.indexOf(ESCAPED_DELIM);
	if (ptr == -1) {
	    return path;
	} else {
	    return path.substring(0, ptr);
	}
    }

    /**
     * Remove the first token from the given path.  The delimiter is the escaped result of getDelimiter().
     */
    protected final String trimToken(String path) {
	int ptr = path.indexOf(ESCAPED_DELIM);
	if (ptr == 0) {
	    return path.substring(1);
	} else if (ptr > 0) {
	    return path.substring(ptr + ESCAPED_DELIM.length());
	} else {
	    return null;
	}
    }

    // Private

    /**
     * Search for a path.  This method converts the path string into tokens delimited by the separator character. Each token
     * is prepended with a ^ and appended with a $.  The method then iterates down the filesystem searching for each token,
     * in sequence, using the Matcher.find method.
     */
    private Collection<String> smartSearch(String path, boolean followLinks) {
	Collection<String> result = new Vector<String>();
	try {
	    if (path.startsWith("^")) {
		path = path.substring(1);
	    }
	    if (path.endsWith("$")) {
		path = path.substring(0, path.length()-1);
	    }
	    StringBuffer sb = new StringBuffer();
	    Iterator<String> iter = StringTools.tokenize(path, getDelimiter(), false);
	    for (int i=0; iter.hasNext(); i++) {
		String token = iter.next();
		if (token.length() > 0) {
		    boolean bound = i > 0 && !".*".equals(token);
		    if (bound && !token.startsWith("^")) {
			sb.append('^');
		    }
		    sb.append(token);
		    if (bound && !token.endsWith("$")) {
			sb.append('$');
		    }
		}
		if (iter.hasNext()) {
		    sb.append(getDelimiter());
		}
	    }
	    result.addAll(smartSearch(null, sb.toString(), followLinks));
	} catch (Exception e) {
	    String msg = e.getMessage() == null ? "null" : e.getMessage();
	    JOVALSystem.getLogger().log(Level.WARNING, msg, e);
	}
	return result;
    }

    /**
     * Search for a path on the filesystem, relative to the given parent path.  All the other search methods ultimately invoke
     * this one.  For the sake of efficiency, this class maintains a map of all the files and directories that it encounters
     * when searching for a path.  That way, it can resolve similar path searches very quickly without having to access the
     * underlying implementation.
     *
     * An alternative approach would have been to maintain a record of previously-encountered searches and cache the result,
     * but the implemented solution is more general, even though it requires specific patterns to be re-matched in the case of
     * repeat searches.
     *
     * @arg parent cannot contain any search strings -- this is a fully-resolved portion of the path.
     * @arg followLinks whether or not filesystem links should be followed by the search.
     * @returns a list of matching local paths
     * @throws FileNotFoundException if a match cannot be found.
     */
    private Collection<String> smartSearch(String parent, String path, boolean followLinks) throws Exception {
	if (path == null || path.length() < 1) {
	    throw new IOException(JOVALSystem.getMessage("ERROR_FS_NULLPATH"));
	}
	String parentName = parent == null ? "[root]" : parent;
	JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_FS_SEARCH", parentName, path));

	INode accessor = null;
	//
	// Advance to the starting position!
	//
	ITree tree = null;
	INode node = null;
	if (parent == null) {
	    String root = getToken(path);
	    tree = cache.getTree(root);
	    if (tree == null) { // first-ever call
		tree = cache.makeTree(root, getDelimiter());
	    }
	    accessor = lookup(root + getDelimiter());
	    node = tree.getRoot();
	    path = trimToken(path);
	} else {
	    String root = getToken(parent);
	    tree = cache.getTree(root);
	    if (tree == null) {
		tree = cache.makeTree(root, getDelimiter());
		node = tree.getRoot();
		accessor = lookup(parent + getDelimiter());
		while ((parent = trimToken(parent)) != null) {
		    node = tree.makeNode(node, getToken(parent));
		}
	    } else {
		node = tree.getRoot();
		try {
		    while ((parent = trimToken(parent)) != null) {
			node = node.getChild(getToken(parent));
		    }
		    accessor = lookup(node.getPath() + getDelimiter());
		} catch (NoSuchElementException e) {
		    do {
			node = tree.makeNode(node, getToken(parent));
		    } while ((parent = trimToken(parent)) != null);
		    accessor = lookup(node.getPath() + getDelimiter());
		}
	    }
	}
	boolean cacheRead = node.getType() == INode.Type.BRANCH;
	List<String> results = new Vector<String>();
	String token = getToken(path);
	path = trimToken(path);
	Collection<INode> children = null;
	if (cacheRead) {
	    children = node.getChildren();
	} else if (accessor.getType() == INode.Type.LINK && !followLinks) {
	    return results;
	} else if (accessor.hasChildren()) {
	    children = accessor.getChildren();
	    for (INode child : children) {
		tree.makeNode(node, child.getName());
	    }
	} else {
	    return results; // end of the line
	}

	Pattern p = Pattern.compile(token);
	for (INode child : children) {
	    if (p.matcher(child.getName()).find()) {
		if (path == null) {
		    results.add(child.getPath());
		} else {
		    results.addAll(smartSearch(child.getPath(), path, followLinks));
		}
	    }
	}
	return results;
    }
}
