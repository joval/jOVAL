// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.apache.jdbm.Serializer;

import org.joval.intf.util.ISearchable;
import org.joval.intf.util.tree.ICacheable;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.INode;
import org.joval.util.StringTools;
import org.joval.util.tree.Forest;
import org.joval.util.tree.Node;
import org.joval.util.tree.Tree;
import org.joval.util.tree.TreeHash;

/**
 * An abstract hierarchy that is intended to serve as a base class for ITree implementations whose access operations are too
 * expensive for direct, repeated use in searches.  The CachingHierarchy stores search results in an in-memory cache for better
 * performance.
 *
 * The CachingHierarchy provides abstract methods that must be implemented by subclasses to populate the cache in bulk, and it
 * also provides internal methods that convert regular expression searches into progressive tree node searches, which are used
 * when the preload methods return false.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CachingHierarchy<T extends ICacheable> implements ISearchable {
    /**
     * Convenience method to create a new JDBM database instance.
     */
    protected static DB makeDatabase(File dir, String name) {
	//
	// Start by cleaning up any files that might have been left by a previous crash
	//
	if (dir.exists() && dir.isDirectory()) {
	    for (File f : dir.listFiles()) {
		if (f.getName().startsWith(name)) {
		    f.delete();
		}
	    }
	}
	DBMaker maker = DBMaker.openFile(new File(dir, name).toString());
	maker.disableTransactions();
	maker.deleteFilesAfterClose();
	maker.closeOnExit();
	return maker.make();
    }

    private String ESCAPED_DELIM;
    private String DELIM;
    private String name;
    private TreeHash<T> cache;
    private Hashtable<String, Exception> irretrievable;

    protected DB db;
    protected Serializer<T> ser;
    protected LocLogger logger;

    /**
     * Instantiate a cache.
     */
    protected CachingHierarchy(String name, String delimiter) {
	this.name = name;
	ESCAPED_DELIM = Matcher.quoteReplacement(delimiter);
	DELIM = delimiter;
	logger = JOVALMsg.getLogger();
	irretrievable = new Hashtable<String, Exception>();
    }

    /**
     * Initialize as an in-memory cache.
     */
    public void init() throws IllegalStateException {
	init(null, null);
    }

    /**
     * Initialize as a JDBM-backed cache.
     */
    public void init(DB db, Serializer<T> ser) throws IllegalStateException {
	if (cache == null) {
	    this.db = db;
	    this.ser = ser;
	    reset();
	} else {
	    throw new IllegalStateException("Already initialized");
	}
    }

    /**
     * Permanently close the cache. It cannot be re-used after being closed; a new instance would be required.
     */
    public void dispose() {
	clear();
	if (db != null) {
	    db.close();
	}
    }

    /**
     * Clear the cache.
     */
    public void clear() {
	cache.clear();
	irretrievable.clear();
    }

    /**
     * Reset the cache to a pristine state.
     */
    public final void reset() {
	if (cache == null) {
	    cache = new TreeHash<T>(name, DELIM, db, ser);
	} else {
	    cache.reset();
	}
	setLogger(logger);
    }

    /**
     * Return the number of items stored in the cache.
     */
    public final int cacheSize() {
	return cache.getRoot().size();
    }

    /**
     * Get the names of all the trees in the cache.
     */
    public final Collection<String> getRoots() {
	Collection<String> roots = new Vector<String>();
	for (ITree tree : cache.getRoot()) {
	    roots.add(tree.getName());
	}
	return roots;
    }

    /**
     * This method exists for the purpose of getting an INode view from inside the cache, which may only be partially
     * constructed. Use this method to:
     *
     * 1) Determine what tree the resource resides in.
     * 2) Traverse links, and get the canonical path of a node that has been reached through a link.
     *
     * @throws NoSuchElementException if there is no data in the cache at the specified path
     */
    public final INode peek(String path) throws NoSuchElementException {
	if (cache.getData(path) == null) {
	    throw new NoSuchElementException(path);
	}
	return (Node)cache.getRoot().lookup(path);
    }

    /**
     * Get a resource from the cache, and if it is not there, get it from the subclass via the accessResource method.
     * The result, if it exists, is guaranteed to be placed in the cache.
     *
     * @throws NoSuchElementException if there is no resource at path
     */
    protected final T getResource(String path) throws Exception {
	try {
	    T resource = cache.getData(path);
	    if (resource == null) {
		throw new NoSuchElementException(path);
	    }
	    return resource;
	} catch (NoSuchElementException e) {
	    logger.debug(JOVALMsg.STATUS_CACHE_ACCESS, path);
	}

	//
	// If the access layer previously failed to retrieve the item, re-throw the original exception.
	//
	if (irretrievable.containsKey(path)) {
	    logger.warn(JOVALMsg.ERROR_CACHE_IRRETRIEVABLE, path);
	    throw new Exception(irretrievable.get(path));
	}

	//
	// The item is not in the cache, and no previous attempt was made to access it, so attempt to retrieve it.
	//
	try {
	    T item = accessResource(path, getDefaultFlags());
	    if (item.exists()) {
		addToCache(path, item);
	    }
	    return item;
	} catch (Exception e) {
	    irretrievable.put(path, e);
	    throw e;
	}
    }

    /**
     * Deposit an item into the cache.
     */
    protected final Node addToCache(String path, T item) {
	Node node = (Node)cache.putData(path, item);
        try {
            if (item.isLink()) {
                ((Tree)node.getTree()).makeLink(node, item.getLinkPath());
            } else if (item.isContainer()) {
                node.setBranch();
		if (item.isAccessible()) {
		    //
		    // Create any descendant child nodes in the cache, so that they can be listed later on without having
		    // to access the item again.
		    //
		    String[] children = listChildren(path);
		    for (String child : children) {
			cache.getCreateNode(node.getPath() + DELIM + child);
		    }
		}
            }
        } catch (UnsupportedOperationException e) {
            logger.debug(e.getMessage());
        } catch (Exception e) {
            logger.debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
        }
	return node;
    }

    /**
     * Add a structural tree to the hierarchy.
     */
    protected final ITree addRoot(String name) {
	return cache.getRoot().makeTree(name);
    }

    // Subclasses must implement the following methods

    protected abstract Serializer<T> getSerializer();

    /**
     * Return the default flags used for internal accessResource calls.
     */
    protected abstract int getDefaultFlags();

    /**
     * Get a resource directly from an underlying access layer implementation.  Subclasses should make NO ATTEMPT to cache
     * the result of this call.  If it is desirable to cache a result, then use the getResource method, which calls this
     * method internally.
     *
     * @throws NoSuchElementException if there is no resource at path
     */
    protected abstract T accessResource(String path, int flags) throws Exception;

    /**
     * Return a list of Strings naming the resources available beneath the specified path.
     *
     * @return null if listing children makes no sense, i.e., the path describes a regular file
     *
     * @throws NoSuchElementException if there is no resource at path
     */
    protected abstract String[] listChildren(String path) throws Exception;

    /**
     * Subclasses should load data into the cache in bulk using addToCache(path, item).  The method should return
     * true if the load is successful, or if the cache has already been successfully loaded.
     */
    protected abstract boolean loadCache();

    // Implement ILogger

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
	cache.getRoot().setLogger(logger);
    }

    // Implement ISearchable

    /**
     * Search the cache, or interpret the pattern and crawl the access layer, caching results along the way.
     */
    public Collection<String> search(Pattern p, int flags) {
	boolean followLinks = (ISearchable.FOLLOW_LINKS == (ISearchable.FOLLOW_LINKS & flags));
	try {
	    String pattern = p.pattern();
	    if (loadCache()) {
		logger.debug(JOVALMsg.STATUS_CACHE_SEARCH, pattern);
		return cache.getRoot().search(p, followLinks);
	    } else {
		//
		// DAS: treeSearch link support is TBD
		//
		logger.debug(JOVALMsg.STATUS_TREESEARCH, pattern);
		return treeSearch(p);
	    }
	} catch (PatternSyntaxException e) {
	    getLogger().warn(JOVALMsg.ERROR_PATTERN, p.pattern());
	    getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalArgumentException e) {
	    getLogger().warn(JOVALMsg.ERROR_TREESEARCH, p.pattern());
	}
	return null;
    }

    // Private

    private static final String[] OPEN = {"{", "(", "["};
    private static final String[] CLOSE = {"}", ")", "]"};

    /**
     * Get the first token from the given path.
     *
     * If the next delimiter is contained by a regex group, then the token is the whole path.
     */
    private final String getToken(String path) {
	if (path.startsWith(ESCAPED_DELIM)) { // special case
	    return ESCAPED_DELIM;
	}

	int ptr = path.indexOf(ESCAPED_DELIM);
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
     * Strip the first token from the given path.
     */
    private final String trimToken(String path) {
	String token = getToken(path);
	if (token.equals(ESCAPED_DELIM)) {
	    return path.substring(token.length());
	} else if (token.equals(path)) {
	    return null;
	} else {
	    if (path.substring(token.length()).startsWith(ESCAPED_DELIM)) {
		return path.substring(token.length() + ESCAPED_DELIM.length());
	    } else {
		getLogger().warn(JOVALMsg.ERROR_TREESEARCH_TOKEN, token, path);
		return null;
	    }
	}
    }

    /**
     * Search for a path.
     */
    private Collection<String> treeSearch(Pattern p) throws IllegalArgumentException {
	Collection<String> result = new HashSet<String>();
	try {
	    String path = p.pattern();
	    if (path.startsWith("^")) {
		for (String s : treeSearch(null, path.substring(1))) {
		    if (p.matcher(s).find()) {
			result.add(s);
		    }
		}
	    } else {
		throw new IllegalArgumentException(path);
	    }
	} catch (Exception e) {
	    getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_TREESEARCH_PATH));
	}
	logger.trace(JOVALMsg.STATUS_FS_SEARCH, parent == null ? "[root]" : parent, path);

	//
	// Advance to the starting position, which is either a root node or the node whose path is specified by parent.
	//
	INode node = null;
	if (parent == null) {
	    parent = getToken(path);
	    path = trimToken(path);
	}
	try {
	    node = cache.getRoot().lookup(parent);
	} catch (NoSuchElementException e) {
	    //
	    // The parent node has never been accessed before, so make sure a matching resource actually exists,
	    // or let an exception exit us from this method.
	    //
	    T item = getResource(parent);
	    node = cache.getRoot().lookup(parent);
	}

	HashSet<String> results = new HashSet<String>();

	//
	// Make sure we're still going someplace...
	//
	if (path == null) {
	    return results;
	}

	//
	// Determine whether the children of the node are already in the cache
	//
	boolean useAccessor = true;
	switch(node.getType()) {
	  case LINK:
	    useAccessor = false; //DAS: we handle this inside the tree, right?
	    break;
	  case BRANCH:
	    useAccessor = null == cache.getData(node.getPath());
	    break;
	  case TREE:
	    useAccessor = null == cache.getData(node.getName());
	    break;
	  case LEAF:
	    if (null != cache.getData(node.getPath())) {
		//
		// If we've made it here, there is data in the cache for this node, so we know it's a bona-fide
		// leaf, so return.
		//
		return results;
	    }
	    break;
	}
	if (useAccessor) {
	    //
	    // ... they aren't, so add the node's children to the cache
	    //
	    try {
		String listPath = null;
		if (node instanceof ITree) {
		    listPath = node.getName();
		} else {
		    listPath = node.getPath();
		}
		String[] children = listChildren(listPath);
		if (children == null || children.length == 0) {
		    //
		    // This node has no children, so stop searching it!
		    //
		    return results;
		} else {
		    for (String child : children) {
			try {
			    getResource(node.getPath() + DELIM + child);
			} catch (Exception e) {
			}
		    }
		}
	    } catch (UnsupportedOperationException e) {
		return results; // accessor is a leaf
	    } catch (NoSuchElementException e) {
		// TBD (DAS): the node has disappeared since being discovered?
	    }
	}

	//
	// Search the node's children for the next token in the search path.
	//
	Collection<INode> children = node.getChildren();
	String token = getToken(path);
	path = trimToken(path);
	if (StringTools.containsUnescapedRegex(token)) {
	    //
	    // If there is no path remaining, only the current regular expression token matters.
	    //
	    if (path == null) {
		if (".*".equals(token)) {
		    for (INode child : children) {
			results.add(child.getPath());
			results.addAll(treeSearch(child.getPath(), ".*"));
		    }
		    return results;
		} else if (token.endsWith(".*") || token.endsWith(".*$") ||
			   token.endsWith(".+") || token.endsWith(".+$")) {
    
		    StringBuffer sb = new StringBuffer(node.getPath()).append(DELIM);
		    if (token.endsWith("$")) {
			sb.append(token.substring(0, token.length() - 3));
		    } else {
			sb.append(token.substring(0, token.length() - 2));
		    }
		    String prefix = sb.toString();
		    if (!StringTools.containsUnescapedRegex(prefix)) {
			for (INode child : children) {
			    if (child.getPath().startsWith(prefix)) {
				if (token.endsWith(".*")) {
				    results.add(child.getPath());
				} else if (token.endsWith(".+") && child.getPath().length() > prefix.length()) {
				    results.add(child.getPath());
				}
				results.addAll(treeSearch(child.getPath(), ".*"));
			    }
			}
			return results;
		    }
		}
	    }

	    //
	    // General-purpose algorithm: recursively gather all children, to be filtered upon return.
	    //
	    for (INode child : children) {
		if (!results.contains(child.getPath())) {
		    results.add(child.getPath());
		}
		results.addAll(treeSearch(child.getPath(), ".*"));
	    }
	} else {
	    //
	    // The token contains only simple regex that can be matched against the children
	    //
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
	}

	return results;
    }

    protected void save(File f) {
	try {
	    PrintStream out = new PrintStream(f);
	    for (ITree tree : cache.getRoot()) {
		out.println("# New Tree: " + tree.getName());
		save(tree, out);
	    }
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private void save(INode node, PrintStream out) {
	switch(node.getType()) {
	  case LEAF:
	    out.print("-");
	    break;
	  case TREE:
	  case BRANCH:
	  case FOREST:
	    out.print("d");
	    break;
	  case LINK:
	    out.print("l");
	    break;
	}
	if (cache.getData(node.getCanonicalPath()) != null) {
	    out.print(" [data]  ");
	} else {
	    out.print(" [empty] ");
	}
	out.print(node.getPath());
	if (node.getType() == INode.Type.LINK) {
	    out.print(" -> ");
	    out.print(node.getCanonicalPath());
	}
	out.println("");
	try {
	    if (node.hasChildren()) {
		for (INode child : node.getChildren()) {
		    save(child, out);
		}
	    }
	} catch (NoSuchElementException e) {
	    System.out.println("Dead link: " + node.getPath());
	}
    }
}
