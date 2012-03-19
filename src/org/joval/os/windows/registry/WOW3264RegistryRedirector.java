// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.util.JOVALMsg;
import org.joval.util.PropertyHierarchy;
import org.joval.util.StringTools;

/**
 * Implementation of an IPathRedirector for a registry.
 *
 * See http://msdn.microsoft.com/en-us/library/aa384253(v=vs.85).aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WOW3264RegistryRedirector implements IPathRedirector {
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
		    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	    IKey key = reg.fetchKey(IRegistry.HKLM, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion");
	    String productName = ((IStringValue)reg.fetchValue(key, "ProductName")).getData();
	    for (Flavor flavor : Flavor.values()) {
		Pattern p = flavor.getPattern();
		if (p != null && p.matcher(productName).find()) {
		    return flavor;
		}
	    }
	    reg.getLogger().warn(JOVALMsg.STATUS_WINREG_REDIRECT_UNSUPPORTED, productName);
	} catch (NoSuchElementException e) {
	    reg.getLogger().error(JOVALMsg.ERROR_WINREG_FLAVOR, e.getMessage());
	}
	return Flavor.UNSUPPORTED;
    }

    private Flavor flavor;

    /**
     * Create a new redirector.
     */
    public WOW3264RegistryRedirector(Flavor flavor) {
	this.flavor = flavor;
    }

    private static final String WOW6432NODE		= "Wow6432Node";
    private static final String SOFTWARE		= "SOFTWARE"+IRegistry.DELIM_STR;
    private static final String FULL_SOFTWARE_STR	= IRegistry.HKLM+IRegistry.DELIM_STR+SOFTWARE;
    private static final int FULL_SOFTWARE_LEN		= FULL_SOFTWARE_STR.length();
    private static final String REDIR_SOFTWARE_STR	= FULL_SOFTWARE_STR+WOW6432NODE+IRegistry.DELIM_STR;
    private static final int REDIR_SOFTWARE_LEN		= REDIR_SOFTWARE_STR.length();

    // Implement IPathRedirector

    public String getRedirect(String path) {
	if (path.startsWith(IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Wow6432Node")) {
	    return null; // already redirected
	}

	switch(flavor) {
	  case WIN7:
	  case WIN2008R2:
	    if (POLICY_REDIRECTED.equals(ph.getProperty(path, KEY_WIN7_08R2))) {
		return splice(path);
	    } else {
		return null;
	    }

	  case VISTA:
	  case WIN2008:
	  case WIN2003:
	  case WINXP:
	    if (POLICY_REDIRECTED.equals(ph.getProperty(path, KEY_LEGACY))) {
		return splice(path);
	    } else {
		return null;
	    }
	}
	return null;
    }

    // Private

    private static final String KEY_WIN7_08R2		= "latest";	// Windows 7, Windows 2008 R2
    private static final String KEY_LEGACY		= "legacy";	// the rest
    private static final String POLICY_REDIRECTED	= "redirect";
    private static final String POLICY_SHARED		= "shared";

    private static PropertyHierarchy ph = new PropertyHierarchy(IRegistry.DELIM_STR);
    static {
	String path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\Appid";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);					// DAS: exceptions TBD
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\CLSID";		// DAS: exceptions TBD
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\DirectShow";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\HCP";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\Interface";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\Media Type";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\MediaFoundation";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Clients";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\COM3";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Cryptography\\Calais\\Current";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Cryptography\\Calais\\Readers";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Cryptography\\Services";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CTF\\SystemShared";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CTF\\TIP";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\DFS";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Driver Signing";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\EnterpriseCertificates";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\EventSystem";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\MSMQ";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Non-Driver Signing";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Nodepad\\DefaultFonts";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\OLE";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\RAS";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\RPC";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\SOFTWARE\\Microsoft\\Shared Tools\\MSInfo";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\System Certificates";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\TermServLicensing";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\TransactionServer";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\App Paths";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Control Panel\\Cursors\\Schemes";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Explorer\\AutoplayHandlers";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Explorer\\DriveIcons";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Explorer\\KindMap";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Group Policy";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Policies";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\PreviewHandlers";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Setup";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\CurrentVersion\\Telephony Locations";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Console";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\FontDpi";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\FontLink";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\FontMapper";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Fonts";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\FontSubstitutes";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Gre_Initialize";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Language Pack";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\NetworkCards";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Ports";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Print";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\TimeZones";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\Policies";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);
	path = IRegistry.HKLM + IRegistry.DELIM_STR + "SOFTWARE\\RegisteredApplications";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_SHARED);

	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\Appid";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_SHARED);				// DAS: exceptions TBD
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\CLSID";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\DirectShow";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\Interface";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\Media Type";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
	path = IRegistry.HKCU + IRegistry.DELIM_STR + "SOFTWARE\\Classes\\MediaFoundation";
	ph.setProperty(path,	KEY_WIN7_08R2,	POLICY_REDIRECTED);
	ph.setProperty(path,	KEY_LEGACY,	POLICY_REDIRECTED);
    }

    // Private

    private String splice(String path) {
	int ptr = path.indexOf("\\SOFTWARE\\");
	if (ptr == -1) {
	    return path + IRegistry.DELIM_STR + "Wow6432Node";
	} else {
	    ptr = ptr + 9;
	    String s = path.substring(0, ptr) + IRegistry.DELIM_STR + "Wow6432Node" + path.substring(ptr);
	    return s;
	}
    }
}
