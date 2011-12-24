// Copyright (C) 2011 jOVAL.org.  All rights reserved.

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

// REMIND (DAS): migrate these timeouts to JOVALSystem properties.
    long TIMEOUT_S	=   15000L;	// 15 sec
    long TIMEOUT_M	=  120000L;	//  2 min
    long TIMEOUT_L	=  900000L;	// 15 min
    long TIMEOUT_XL	= 3600000L;	//  1 hr

    Flavor getFlavor();

    /**
     * Enumeration of Unix flavors.
     */
    enum Flavor {
	UNKNOWN("unknown"),
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
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_UNIX_FLAVOR);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    return flavor;
	}
    }


}
