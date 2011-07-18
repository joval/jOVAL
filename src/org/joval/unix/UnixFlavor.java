// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix;

/**
 * Enumeration of Unix flavors.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
enum UnixFlavor {
    UNKNOWN("unknown"),
    LINUX("Linux"),
    SOLARIS("SunOS");

    private String osName = null;

    private UnixFlavor(String osName) {
	this.osName = osName;
    }

    String osName() {
	return osName;
    }
}
