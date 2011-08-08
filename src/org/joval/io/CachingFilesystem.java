// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IPathRedirector;
import org.joval.intf.io.IRandomAccess;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.TreeNode;

/**
 * An abstract IFilesystem implementation that caches search results for better performance.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class CachingFilesystem implements IFilesystem, IPathRedirector {
    private Map <String, TreeNode>cache;
    private boolean caseInsensitive;

    public CachingFilesystem() {
	cache = new Hashtable<String, TreeNode>();
	caseInsensitive = false;
    }

    // Implement IFilesystem (sparsely)

    public abstract String getDelimString();

    public List<String> search(String path) throws IOException {
	return search(null, path);
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
     * @arg sf set to null for an absolute path search, or non-null for a relative search.
     * @returns a list of matching local paths
     * @throws FileNotFoundException if a match cannot be found.
     */
    public List<String> search(String parent, String path) throws IOException {
	if (path == null || path.length() < 1) {
	    throw new IOException(JOVALSystem.getMessage("ERROR_FS_NULLPATH"));
	} else if (parent == null && path.startsWith("^")) {
	    path = path.substring(1);
	}

	//
	// Advance to the starting position!
	//
	IFile file = null;
	TreeNode node = null;
	if (parent == null) {
	    String root = getToken(path);
	    node = cache.get(root);
	    if (node == null) { // first-ever call
	        node = TreeNode.makeRoot(root, getDelimString());
	        cache.put(root, node);
	        file = getFile(root + getDelimString());
	    }
	    path = trimToken(path);
	} else {
	    String root = getToken(parent);
	    node = cache.get(root);
	    if (node == null) {
	        node = TreeNode.makeRoot(root, getDelimString());
	        cache.put(root, node);
	        file = getFile(parent + getDelimString());
	        while ((parent = trimToken(parent)) != null) {
	            node = TreeNode.makeNode(node, getToken(parent));
	        }
	    } else {
	        try {
	            while ((parent = trimToken(parent)) != null) {
	                node = node.getChild(getToken(parent));
	            }
	            file = getFile(node.toString() + getDelimString());
	        } catch (NoSuchElementException e) {
	            do {
	                node = TreeNode.makeNode(node, getToken(parent));
	            } while ((parent = trimToken(parent)) != null);
	            file = getFile(node.toString() + getDelimString());
	        }
	    }
	}
	boolean cacheRead = node.isBranch();
	List<String> results = new Vector<String>();
	String token = getToken(path);
	path = trimToken(path);
	String[] children = null;
	if (cacheRead) {
	    children = node.list();
	} else if (file.isDirectory()) {
	    children = file.list();
	    for (int i=0; i < children.length; i++) {
	        TreeNode.makeNode(node, children[i]);
	    }
	} else {
	    return results; // end of the line
	}

	String patternStr = null;
	if (caseInsensitive) {
	    patternStr = "(?i)" + token;
	} else {
	    patternStr = token;
	}
	Pattern p = Pattern.compile(patternStr);
	for (int i=0; i < children.length; i++) {
	    Matcher m = p.matcher(children[i]);
	    //
	    // Does a match for a path token, but a find for the filename (last token).
	    // DAS: this is not exactly correct.. look for a .* in the token?
	    //
	    if ((path == null && m.find()) || m.matches()) {
		TreeNode child = node.getChild(children[i]);
		if (path == null) {
		    results.add(child.toString());
		} else {
		    results.addAll(search(child.toString(), path));
		}
	    }
	}
	return results;
    }

    // Subclasses-only

    /**
     * By default, the cache is case-sensitive.  Subclasses that are not case-sensitive (i.e., Windows) should set the flag
     * to true using this method.
     */
    protected void setCaseInsensitive(boolean caseInsensitive) {
	this.caseInsensitive = caseInsensitive;
    }

    // Private

    private String getToken(String path) {
	int ptr = path.indexOf(Matcher.quoteReplacement(getDelimString()));
	if (ptr != -1) {
	    return path.substring(0, ptr);
	} else {
	    return path;
	}
    }

    private String trimToken(String path) {
	String s = Matcher.quoteReplacement(getDelimString());
	int ptr = path.indexOf(s);
	if (ptr == 0) {
	    return path.substring(1);
	} else if (ptr > 0) {
	    return path.substring(ptr + s.length());
	} else {
	    return null;
	}
    }
}
