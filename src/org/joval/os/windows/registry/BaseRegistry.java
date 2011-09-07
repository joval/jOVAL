// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IRegistryRedirector;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.system.Environment;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A base class for accessing the Windows registry over the network.   This class handles searching, caching searches
 * and 64-bit redirection.
 *
 * DAS: Need to complete the implementation of Wow64 registry redirection; see:
 *      http://msdn.microsoft.com/en-us/library/aa384253%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseRegistry implements IRegistry {
    protected Environment env = null;
    protected boolean ia64 = false;
    protected IRegistryRedirector redirector;
    protected Hashtable<String, List<IKey>> searchMap;

    /**
     * Create a new Registry, connected to the specified host using the specified Credential.
     */
    protected BaseRegistry() {
	searchMap = new Hashtable<String, List<IKey>>();
	redirector = new DefaultRedirector();
    }

    // Implement IRegistry (sparsely)

    /**
     * Retrieve the remote machine's environment.  This is a combination of the SYSTEM environment and the logged-in
     * USER environment (that is, the user in the the Credential that was provided to the constructor).
     */
    public IEnvironment getEnvironment() throws IllegalStateException {
	if (env == null) {
	    throw new IllegalStateException(JOVALSystem.getMessage("ERROR_WINREG_STATE"));
	}
	return env;
    }

    /**
     * If this is a 64-bit registry, should redirection be implemented?  (That is, should we pretend that the user of
     * this class behaves like a 32-bit Windows application, having no knownedge of the 64-bit registry behavior?)
     *
     * This setting is ignored if not connected to a 64-bit registry.
     */
    public void setRedirector(IRegistryRedirector redirector) {
	this.redirector = redirector;
    }

    /**
     * Do we have a 64-bit view of the registry?
     */
    public boolean is64Bit() {
	return ia64;
    }

    /**
     * Searches for the specified path under the specified hive.
     */
    public List<IKey> search(String hive, String path) throws NoSuchElementException {
	return searchInternal(getHive(hive), cleanRegex(path), redirector.isEnabled());
    }

    public List<IKey> search(String hive, String path, boolean win32) throws NoSuchElementException {
	return searchInternal(getHive(hive), cleanRegex(path), win32);
    }

    // Private

    private List<IKey> searchInternal(IKey key, String path, boolean win32) throws NoSuchElementException {
	String cacheKey = "search: " + key.toString() + DELIM_STR + path;
	List<IKey> list = searchMap.get(cacheKey);
	if (list != null) {
	    return list;
	}
	list = new Vector<IKey>();

/*
	if (win32) {
	    String alt = redirector.getRedirect(key.toString());
	    if (alt != null) {
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_WINREG_REDIRECT", key.toString(), alt));
		key = fetchKey(alt, false);
	    }
	}
*/
	String next = null;
	int ptr = path.indexOf("\\\\");
	if (ptr > 0) {
	    next = path.substring(0, ptr);
	    path = path.substring(ptr+2);
	} else {
	    next = path;
	    path = null;
	}

	try {
	    if (!next.startsWith("(")) {
		next = "(?i)" + next; // Registry is case-insensitive
	    }
	    Pattern p = Pattern.compile(next);
	    String[] children = key.listSubkeys(p);
	    if (children.length == 0) {
		throw new NoSuchElementException(key.toString() + DELIM_STR + next);
	    } else {
		for (int i=0; i < children.length; i++) {
		    if (path == null) {
			list.add(fetchSubkey(key, children[i], win32));
		    } else {
			try {
			    Iterator<IKey> iter = searchInternal(fetchSubkey(key, children[i], win32), path, win32).iterator();
			    while (iter.hasNext()) {
				list.add(iter.next());
			    }
			} catch (NoSuchElementException e) {
			    // a dead end in the search
			}
		    }
		}
	    }
	} catch (PatternSyntaxException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()), e);
	    throw new NoSuchElementException(e.getMessage());
	}
	searchMap.put(cacheKey, list);
	return list;
    }

    private String cleanRegex(String s) {
	String clean = s;
	int len = clean.length();
	if (len > 1) {
	    if (clean.charAt(0) == '^' && clean.charAt(1) == '(' && clean.charAt(len-1) == '$' && clean.charAt(len-2) == ')') {
		clean = new StringBuffer("^").append(clean.substring(2, len-2)).append('$').toString();
		len = clean.length();
	    }
	    if (len > 2 && clean.charAt(len-1) == '$' && clean.charAt(len-2) == '}' && clean.charAt(len-3) != '\\') {
		int ptr = clean.indexOf('{');
		if (ptr == 0 || ptr == 1 || (ptr > 2 && clean.charAt(ptr-1) == '\\' && clean.charAt(ptr-2) == '\\')) {
		    StringBuffer sb = new StringBuffer(clean.substring(0, ptr));
		    sb.append("\\").append(clean.substring(ptr, len-2)).append("\\}$");
		    clean = sb.toString();
		}
		//len = clean.length(); // Not necessary on the last test, but a reminder if more clean-up is needed...
	    }
	}
	return clean;
    }

    private class DefaultRedirector implements IRegistryRedirector {
	private DefaultRedirector(){}

	public boolean isEnabled() {
	    return false;
	}

	public String getRedirect(String s) {
	    return s;
	}

	public String getOriginal(String s) {
	    return s;
	}
    }
}
