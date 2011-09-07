// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IRegistryRedirector;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.util.JOVALSystem;

/**
 * Implementation of an IRegistryRedirector.
 *
 * See http://msdn.microsoft.com/en-us/library/aa384253(v=vs.85).aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WOW3264RegistryRedirector implements IRegistryRedirector {
    /**
     * An enumeration identifying all of the versions of Windows on which 32-on-64 bit emulation is supported.
     */
    public static enum Flavor {
	UNSUPPORTED	(null),
	WIN7		("(?i)Windows 7"),
	WIN2008R2	("(?i)2008 R2"),
	WIN2008		("2008"),	// NB: intentionally declared after 2008 R2
	VISTA		("(?i)Vista"),
	WIN2003		("2003"),
	WINXP		("(?i)XP");

	private Pattern p;

	private Flavor(String s) {
	    if (s != null) {
		try {
		    p = Pattern.compile(s);
		} catch (PatternSyntaxException e) {
		    JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage(), e));
		}
	    }
	}

	private Pattern getPattern() {
	    return p;
	}
    }

    /**
     * Returns the Flavor of the registry based on the ProductName value under the registry Key
     * HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion.
     */
    public static Flavor getFlavor(IRegistry reg) {
	try {
	    IKey key = reg.fetchKey(IRegistry.HKLM, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", false);
	    String productName = ((IStringValue)reg.fetchValue(key, "ProductName")).getData();
	    for (Flavor flavor : Flavor.values()) {
		Pattern p = flavor.getPattern();
		if (p != null && p.matcher(productName).find()) {
		    return flavor;
		}
	    }
	} catch (NoSuchElementException e) {
	    JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_WINREG_FLAVOR", e.getMessage()));
	}
	return Flavor.UNSUPPORTED;
    }

    private boolean enabled;
    private Flavor flavor;

    /**
     * Create a new redirector.
     *
     * @param enabled Whether or not redirection should be enabled
     */
    public WOW3264RegistryRedirector(boolean enabled, Flavor flavor) {
	this.enabled = enabled;
	switch(flavor) {
	  case UNSUPPORTED:
	    if (enabled) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("STATUS_WINREG_REDIRECT_OVERRIDE", flavor));
	    }
	    this.enabled = false;
	    // fall-through
	  default:
	    this.flavor = flavor;
	    break;
	}
    }

    private static final String WOW6432NODE		= "Wow6432Node";
    private static final String SOFTWARE		= "SOFTWARE"+IRegistry.DELIM_STR;
    private static final String FULL_SOFTWARE_STR	= IRegistry.HKLM+IRegistry.DELIM_STR+SOFTWARE;
    private static final int FULL_SOFTWARE_LEN		= FULL_SOFTWARE_STR.length();
    private static final String REDIR_SOFTWARE_STR	= FULL_SOFTWARE_STR+WOW6432NODE+IRegistry.DELIM_STR;
    private static final int REDIR_SOFTWARE_LEN		= REDIR_SOFTWARE_STR.length();

    // Implement IRegistryRedirector

    public boolean isEnabled() {
	return enabled;
    }

    /**
     * Returns the non-redirected form of the path.
     */
    public String getOriginal(String path) {
	if (enabled && path.startsWith(REDIR_SOFTWARE_STR)) {
	    return FULL_SOFTWARE_STR + path.substring(REDIR_SOFTWARE_LEN);
	}
	return path;
    }

    /**
     * Returns the alternate location of the Key path specified by the invocation path.  This should be a full path,
     * like the kind returned by Key.toString().  The method returns null if no redirected path is available.
    public String getRedirect(String path) {
	if (enabled && path.startsWith(FULL_SOFTWARE_STR)) {
	    StringTokenizer tok = new StringTokenizer(path.substring(FULL_SOFTWARE_LEN), IRegistry.DELIM_STR);
	    if (tok.hasMoreTokens()) {
		String next = tok.nextToken();
		if (!WOW6432NODE.equals(next)) {
		    StringBuffer s = new StringBuffer(FULL_SOFTWARE_STR);
		    s.append(WOW6432NODE).append(IRegistry.DELIM_CH).append(next);
		    while (tok.hasMoreTokens()) {
			s.append(IRegistry.DELIM_CH).append(tok.nextToken());
		    }
		    return s.toString();
		}
	    }
	}
	return null;
    }
     */

    public String getRedirect(String path) {
	if (!enabled) {
	    return null;
	}
	if (path.startsWith(IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Wow6432Node")) {
	    return null; // already redirected
	}

	switch(flavor) {
	  case WIN7:
	  case WIN2008R2:
	    if        (path.startsWith(IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\CLSID")) {
		return splice(path);
	    } else if (path.startsWith(IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes")) {
	    } else if (path.startsWith(IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE")) {
		return splice(path);
	    }
	    break;

	  case VISTA:
	  case WIN2008:
	  case WIN2003:
	  case WINXP:
	    if        (path.startsWith(IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE")) {
		return splice(path);
	    }
	    break;
	}
	return null;
    }

    // Private

    private String splice(String path) {
	String s = path.substring(0, 27) + IRegistry.DELIM_STR + "Wow6432Node" + path.substring(27);
	return s;
    }
}
