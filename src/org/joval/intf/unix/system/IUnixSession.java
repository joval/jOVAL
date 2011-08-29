// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.unix.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALSystem;

/**
 * A representation of a Unix command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixSession extends ISession {
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
		for (Flavor f : values()) {
		    if (f.getOsName().equals(osName)) {
			flavor = f;
			break;
		    }
		}
	    } catch (Exception e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PLUGIN_INTERFACE"), e);
	    }
	    return flavor;
	}
    }
}
