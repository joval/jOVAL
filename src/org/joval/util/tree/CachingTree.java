// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

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
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.util.tree.Forest;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.tree.Tree;

/**
 * An abstract tree that is intended to serve as a base class for ITree implementations whose access operations are too
 * expensive for direct, repeated use in searches.  The CachingTree saves search results in an internal cache for better
 * performance.
 *
 * The CachingTree provides methods (preload and preloadLinks) that should be overridden by subclasses to populate the cache in
 * bulk, and it also provides internal methods that convert regular expression searches into progressive tree node searches,
 * which are used when the preload methods return false.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CachingTree implements ITree {
    private String ESCAPED_DELIM;
    protected IForest cache;

    public CachingTree() {
	cache = new Forest();
	ESCAPED_DELIM = Matcher.quoteReplacement(getDelimiter());
    }

    protected boolean preload() {
	return false;
    }

    protected boolean preloadLinks() {
	return false;
    }

    // Implement ITree (sparsely) -- subclasses must implement the getDelimiter and lookup methods.

    public INode getRoot() {
	throw new UnsupportedOperationException();
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
	    return treeSearch(p.pattern(), followLinks);
	}
    }

    // Internal

    /**
     * Get the first token from the given path.  The delimiter is the escaped result of getDelimiter().
     */
    protected final String getToken(String path, String delim) {
	int ptr = path.indexOf(delim);
	if (ptr == -1) {
	    return path;
	} else {
	    return path.substring(0, ptr);
	}
    }

    /**
     * Remove the first token from the given path.  The delimiter is the escaped result of getDelimiter().
     */
    protected final String trimToken(String path, String delim) {
	int ptr = path.indexOf(delim);
	if (ptr == 0) {
	    return path.substring(1);
	} else if (ptr > 0) {
	    return path.substring(ptr + delim.length());
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
    private Collection<String> treeSearch(String path, boolean followLinks) {
	Collection<String> result = new Vector<String>();
	try {
	    if (path.startsWith("^")) {
		path = path.substring(1);
	    }
	    if (path.endsWith("$")) {
		path = path.substring(0, path.length()-1);
	    }
	    StringBuffer sb = new StringBuffer();
	    Iterator<String> iter = StringTools.tokenize(path, ESCAPED_DELIM, false);
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
		    sb.append(ESCAPED_DELIM);
		}
	    }
	    result.addAll(treeSearch(null, sb.toString(), followLinks));
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
    private Collection<String> treeSearch(String parent, String path, boolean followLinks) throws Exception {
	if (path == null || path.length() < 1) {
	    throw new IOException(JOVALSystem.getMessage("ERROR_FS_NULLPATH"));
	}
	String parentName = parent == null ? "[root]" : parent;
	JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_FS_SEARCH", parentName, path));

	INode accessor = null;
	//
	// Advance to the starting position!
	//
	ITreeBuilder tree = null;
	INode node = null;
	if (parent == null) {
	    String root = getToken(path, ESCAPED_DELIM);
	    tree = cache.getTreeBuilder(root);
	    if (tree == null) { // first-ever call
		tree = new Tree(root, getDelimiter());
		cache.addTree(tree);
	    }
	    node = tree.getRoot();
	    path = trimToken(path, ESCAPED_DELIM);
	} else {
	    String root = getToken(parent, getDelimiter());
	    tree = cache.getTreeBuilder(root);
	    if (tree == null) {
		tree = new Tree(root, getDelimiter());
		cache.addTree(tree);
		node = tree.getRoot();
		while ((parent = trimToken(parent, getDelimiter())) != null) {
		    node = tree.makeNode(node, getToken(parent, getDelimiter()));
		}
	    } else {
		node = tree.getRoot();
		try {
		    while ((parent = trimToken(parent, getDelimiter())) != null) {
			node = node.getChild(getToken(parent, getDelimiter()));
		    }
		} catch (NoSuchElementException e) {
		    do {
			node = tree.makeNode(node, getToken(parent, getDelimiter()));
		    } while ((parent = trimToken(parent, getDelimiter())) != null);
		} catch (UnsupportedOperationException e) {
		    do {
			node = tree.makeNode(node, getToken(parent, getDelimiter()));
		    } while ((parent = trimToken(parent, getDelimiter())) != null);
		}
	    }
	}
	boolean cacheRead = node.getType() == INode.Type.BRANCH;
	List<String> results = new Vector<String>();
	Collection<INode> children = null;
	if (cacheRead) {
	    children = node.getChildren();
	} else {
	    String nodePath = node.getPath();
	    if (nodePath.length() == 0) {
		nodePath = getDelimiter();
	    }
	    try {
		accessor = lookup(nodePath);
		if (accessor.getType() == INode.Type.LINK && !followLinks) {
		    return results;
		}
	    } catch (IllegalArgumentException e) {
	    }
	    if (!nodePath.endsWith(getDelimiter())) {
		accessor = lookup(nodePath + getDelimiter());
	    }
	    if (accessor.hasChildren()) {
		children = accessor.getChildren();
		for (INode child : children) {
		    tree.makeNode(node, child.getName());
		}
	    } else {
		return results; // end of the line
	    }
	}

	String token = getToken(path, ESCAPED_DELIM);
	path = trimToken(path, ESCAPED_DELIM);
	Pattern p = Pattern.compile(token);
	for (INode child : children) {
	    if (p.matcher(child.getName()).find()) {
		if (path == null) {
		    results.add(child.getPath());
		} else {
		    results.addAll(treeSearch(child.getPath(), path, followLinks));
		}
	    }
	}
	return results;
    }
}
