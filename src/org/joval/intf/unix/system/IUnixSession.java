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

    long TIMEOUT_S	= JOVALSystem.getLongProperty(JOVALSystem.PROP_UNIX_READ_TIMEOUT_S);
    long TIMEOUT_M	= JOVALSystem.getLongProperty(JOVALSystem.PROP_UNIX_READ_TIMEOUT_M);
    long TIMEOUT_L	= JOVALSystem.getLongProperty(JOVALSystem.PROP_UNIX_READ_TIMEOUT_L);
    long TIMEOUT_XL	= JOVALSystem.getLongProperty(JOVALSystem.PROP_UNIX_READ_TIMEOUT_XL);

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
    
	private String osName = null;
    
	private Flavor(String osName) {
	    this.osName = osName;
	}

	public String getOsName() {
	    return osName;
	}
    
	public static Flavor flavorOf(IUnixSession session) {
	    Flavor flavor = UNKNOWN;
	    try {
		String osName = SafeCLI.exec("uname -s", session, TIMEOUT_S);
		for (Flavor f : values()) {
		    if (f.getOsName().equals(osName)) {
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
