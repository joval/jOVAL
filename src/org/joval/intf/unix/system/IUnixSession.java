// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.unix.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A representation of a Unix command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixSession extends ISession {
    long TIMEOUT_S	=   15000L;	// 15 sec
    long TIMEOUT_M	=  120000L;	//  2 min
    long TIMEOUT_L	=  900000L;	// 15 min
    long TIMEOUT_XL	= 3600000L;	//  1 hr

    boolean DEBUG = false;

    Flavor getFlavor();

    /**
     * Create a process that should last no longer than the specified number of milliseconds.
     */
    IProcess createProcess(String command, long millis, boolean debug) throws Exception;

    /**
     * Enumeration of Unix flavors.
     */
    enum Flavor {
	UNKNOWN("unknown"),
	LINUX("Linux"),
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
		IProcess p = session.createProcess("uname -s");
		p.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String osName = reader.readLine();
		reader.close();
		p.waitFor(0);
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
