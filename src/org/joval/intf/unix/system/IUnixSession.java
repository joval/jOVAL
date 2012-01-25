// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.system;

import java.io.EOFException;

import org.joval.intf.io.IReader;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * A representation of a Unix command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixSession extends ISession {
    /**
     * Property indicating the number of milliseconds to wait for a read before quiting.
     */
    String PROP_SUDO_READ_TIMEOUT = "sudo.read.timeout";

    String PROP_READ_TIMEOUT_S = "read.timeout.small";
    String PROP_READ_TIMEOUT_M = "read.timeout.medium";
    String PROP_READ_TIMEOUT_L = "read.timeout.large";
    String PROP_READ_TIMEOUT_XL = "read.timeout.xl";

    long TIMEOUT_S	= 15000L;
    long TIMEOUT_M	= 120000L;
    long TIMEOUT_L	= 900000L;
    long TIMEOUT_XL	= 3600000L;

    Flavor getFlavor();

    /**
     * Enumeration of Unix flavors.
     */
    enum Flavor {
	UNKNOWN("unknown"),
	AIX("AIX"),
	LINUX("Linux"),
	MACOSX("Darwin"),
	SOLARIS("SunOS");
    
	private String value = null;
    
	private Flavor(String value) {
	    this.value = value;
	}

	public String value() {
	    return value;
	}
    
	public static Flavor flavorOf(IUnixSession session) {
	    Flavor flavor = UNKNOWN;
	    try {
		String osName = SafeCLI.exec("uname -s", session, TIMEOUT_S);
		for (Flavor f : values()) {
		    if (f.value().equals(osName)) {
			flavor = f;
			break;
		    }
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.ERROR_UNIX_FLAVOR);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    return flavor;
	}
    }


}
