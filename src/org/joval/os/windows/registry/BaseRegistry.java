// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.system.Environment;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A base class for accessing the Windows registry.   This class handles searching, caching searches and 64-bit redirection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseRegistry implements IRegistry {
    protected IPathRedirector redirector;
    protected ILoggable log;
    protected Environment env = null;
    protected Hashtable<String, List<IKey>> searchMap;

    /**
     * Create a new Registry, connected to the specified host using the specified Credential.
     */
    protected BaseRegistry(IPathRedirector redirector, ILoggable log) {
	this.redirector = redirector;
	this.log = log;
	searchMap = new Hashtable<String, List<IKey>>();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return log.getLogger();
    }

    public void setLogger(LocLogger logger) {
	log.setLogger(logger);
    }

    // Implement IRegistry (sparsely)

    /**
     * Retrieve the remote machine's environment.  This is a combination of the SYSTEM environment and the logged-in
     * USER environment (that is, the user in the the Credential that was provided to the constructor).
     */
    public IEnvironment getEnvironment() throws IllegalStateException {
	if (env == null) {
	    throw new IllegalStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_STATE));
	}
	return env;
    }

    public List<IKey> search(String hive, String path) throws NoSuchElementException {
	return searchInternal(getHive(hive), cleanRegex(path));
    }

    // Private

    private List<IKey> searchInternal(IKey key, String path) throws NoSuchElementException {
	String cacheKey = "search: " + key.toString() + DELIM_STR + path;
	List<IKey> list = searchMap.get(cacheKey);
	if (list != null) {
	    return list;
	}
	list = new Vector<IKey>();

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
			list.add(fetchSubkey(key, children[i]));
		    } else {
			try {
			    Iterator<IKey> iter = searchInternal(fetchSubkey(key, children[i]), path).iterator();
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
	    log.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new NoSuchElementException(cacheKey);
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
}
