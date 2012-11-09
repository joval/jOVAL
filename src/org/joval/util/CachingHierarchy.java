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
 * expensive for direct, repeated use.  The CachingHierarchy stores search results in a JDBM-backed cache for better
 * performance.
 *
 * The CachingHierarchy provides abstract methods to access underlying resources that must be implemented by subclasses.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CachingHierarchy<T extends ICacheable> {
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

    private String name;
    private TreeHash<T> cache;
    private Hashtable<String, Exception> irretrievable;

    protected DB db;
    protected Serializer<T> ser;
    protected LocLogger logger;
    protected final String ESCAPED_DELIM;
    protected final String DELIM;

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

    // Implement ILogger

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
	cache.getRoot().setLogger(logger);
    }
}
