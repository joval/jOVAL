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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.util.ILoggable;
import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.util.tree.Forest;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.tree.Tree;

/**
 * An abstract tree that is intended to serve as a base class for ITree implementations whose access operations are too
 * expensive for direct, repeated use in searches.  The CachingTree stores search results in an in-memory cache for better
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

    public Collection<String> search(Pattern p) {
	try {
	    if (preload()) {
		return cache.search(p);
	    } else {
		return treeSearch(p.pattern());
	    }
	} catch (PatternSyntaxException e) {
	    getLogger().warn(JOVALMsg.ERROR_PATTERN, p.pattern());
	    getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalArgumentException e) {
	    getLogger().warn(JOVALMsg.ERROR_TREESEARCH, p.pattern());
	}
	return null;
    }

    // Internal

    private static final String[] OPEN = {"{", "(", "["};
    private static final String[] CLOSE = {"}", ")", "]"};

    /**
     * Get the first token from the given path.  The delimiter is the escaped result of getDelimiter().
     *
     * If the next delimiter is contained by a regex group, then the token is the whole path.
     */
    protected final String getToken(String path, String delim) {
	int ptr = path.indexOf(delim);
	if (ptr == -1) {
	    return path;
	}

	int groupPtr = -1;
	for (String s : OPEN) {
	    int candidate = path.indexOf(s);
	    if (candidate > ptr) {
		continue;
	    } else if (candidate < groupPtr) {
		groupPtr = candidate;
	    }
	}

	if (-1 < groupPtr && groupPtr < ptr) {
	    return path;
	} else {
	    return path.substring(0, ptr);
	}
    }

    /**
     * Strip the first token from the given path.  The delimiter is the escaped result of getDelimiter().
     */
    protected final String trimToken(String path, String delim) {
	String token = getToken(path, delim);
	if (token.equals(path)) {
	    return null;
	} else {
	    if (path.substring(token.length()).startsWith(delim)) {
		return path.substring(token.length() + delim.length());
	    } else {
		getLogger().warn(JOVALMsg.ERROR_TREESEARCH_TOKEN, token, path);
		return null;
	    }
	}
    }

    // Private

    /**
     * Search for a path.
     */
    private Collection<String> treeSearch(String path) throws IllegalArgumentException {
	Collection<String> result = new Vector<String>();
	try {
	    if (path.startsWith("^")) {
		result.addAll(treeSearch(null, path.substring(1)));
	    } else {
		throw new IllegalArgumentException(path);
	    }
	} catch (Exception e) {
	    getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return result;
    }

    /**
     * Search for a path on the tree, relative to the given parent path.  All the other search methods ultimately invoke
     * this one.  For the sake of efficiency, this class maintains a map of all the files and directories that it encounters
     * when searching for a path.  That way, it can resolve similar path searches very quickly without having to access the
     * underlying implementation.
     *
     * @arg parent	The parent path, which is a fully-resolved portion of the path (regex-free).
     * @arg path	The search pattern, consisting of ESCAPED_DELIM-delimited tokens for matching node names.
     *
     * @returns a list of matching local paths
     *
     * @throws FileNotFoundException if a match cannot be found.
     */
    private Collection<String> treeSearch(String parent, String path) throws Exception {
	if (path == null || path.length() < 1) {
	    throw new IOException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_NULLPATH));
	}
	String parentName = parent == null ? "[root]" : parent;
	getLogger().trace(JOVALMsg.STATUS_FS_SEARCH, parentName, path);

	INode accessor = null;
	//
	// Advance to the starting position, which is either a root node or the node whose path is specified by parent.
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

	//
	// Discover the node's children using the accessor, or fetch them from the cache.
	//
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
	    } catch (IllegalArgumentException e) {
	    } catch (NoSuchElementException e) {
		// the node has disappeared since being discovered
	    }
	    if (!nodePath.endsWith(getDelimiter())) {
		try {
		    accessor = lookup(nodePath + getDelimiter());
		} catch (NoSuchElementException e) {
		    return results; // accessor is a leaf
		}
	    }
	    try {
		if (accessor.hasChildren()) {
		    children = accessor.getChildren();
		    for (INode child : children) {
			tree.makeNode(node, child.getName());
		    }
		} else {
		    return results; // end of the line
		}
	    } catch (UnsupportedOperationException e) {
		return results; // accessor is a leaf
	    }
	}

	//
	// Search the children for the next token in the search path.
	//
	String token = getToken(path, ESCAPED_DELIM);
	path = trimToken(path, ESCAPED_DELIM);

	//
	// The token contains only simple regex that can be matched against the children
	//
	if (!StringTools.containsUnescapedRegex(token)) {
	    Pattern p = Pattern.compile(token);
	    for (INode child : children) {
	 	if (p.matcher(child.getName()).matches()) {
		    if (path == null) {
			results.add(child.getPath());
		    } else {
			results.addAll(treeSearch(child.getPath(), path));
		    }
		}
	    }

	//
	// Process the final token if it contains an active regular expression
	//
	} else {
	    //
	    // The wildcard case causes a resursive search of all children
	    //
	    if (".*".equals(token)) {
		for (INode child : children) {
		    results.add(child.getPath());
		    results.addAll(treeSearch(child.getPath(), ".*"));
		}
	    } else {
		//
		// Optimization for simple wildcard-terminated searches
		//
		if (token.endsWith(".*") || token.endsWith(".*$") ||
		    token.endsWith(".+") || token.endsWith(".+$")) {

		    StringBuffer sb = new StringBuffer(node.getPath()).append(getDelimiter());
		    if (token.endsWith("$")) {
			sb.append(token.substring(0, token.length() - 3));
		    } else {
			sb.append(token.substring(0, token.length() - 2));
		    }
		    String prefix = sb.toString();
		    if (!StringTools.containsUnescapedRegex(prefix)) {
			for (INode child : children) {
			    String childPath = child.getPath();
			    if (childPath.startsWith(prefix)) {
				if (token.endsWith(".*")) {
				    results.add(childPath);
				} else if (token.endsWith(".+") && childPath.length() > prefix.length()) {
				    results.add(childPath);
				}
				results.addAll(treeSearch(childPath, ".*"));
			    }
			}
			return results;
		    }
		}

		//
		// General-purpose algorithm: recursively gather all children, then filter for matches
		//
		Vector<String> candidates = new Vector<String>();
		for (INode child : children) {
		    candidates.add(child.getPath());
		    candidates.addAll(treeSearch(child.getPath(), ".*"));
		}
 
		Pattern p = Pattern.compile(StringTools.escapeRegex(node.getPath()) + ESCAPED_DELIM + token);
		for (String candidate : candidates) {
		    if (p.matcher(candidate).matches()) {
			results.add(candidate);
		    }
		}
	    }
	}

	return results;
    }
}
