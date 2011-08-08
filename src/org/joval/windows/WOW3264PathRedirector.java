// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows;

import org.joval.intf.io.IPathRedirector;
import org.joval.intf.system.IEnvironment;

/**
 * Implementation of an IPathRedirector for the Windows IFilesystems.
 *
 * @see http://msdn.microsoft.com/en-us/library/aa384187%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WOW3264PathRedirector implements IPathRedirector {
    static final String SEPARATOR = "\\";

    private String system32, sysWOW64, sysNative;

    public WOW3264PathRedirector(IEnvironment env) {
	system32 = env.getenv("SystemRoot") + SEPARATOR + "System32" + SEPARATOR;
	sysNative = env.getenv("SystemRoot") + SEPARATOR + "Sysnative" + SEPARATOR;
	sysWOW64 = env.getenv("SystemRoot") + SEPARATOR + "SysWOW64" + SEPARATOR;
    }

    // Implement IPathRedirector

    public String getRedirect(String path) {
	if (path.toUpperCase().startsWith(system32.toUpperCase())) {
	    return sysWOW64 + path.substring(system32.length());
	} else if (path.toUpperCase().startsWith(sysNative.toUpperCase())) {
	    return system32 + path.substring(system32.length());
	} else {
	    return path;
	}
    }
}
