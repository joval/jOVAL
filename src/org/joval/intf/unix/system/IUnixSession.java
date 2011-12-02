// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.unix.system;

import java.io.EOFException;
import java.io.InputStream;

import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.io.StreamTool;
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
	MACOSX("Darwin"),
	SOLARIS("SunOS");
    
	private String osName = null;
    
	private Flavor(String osName) {
	    this.osName = osName;
	}

	public String getOsName() {
	    return osName;
	}
    
	private static long readTimeout = 5000L;
	private static int execRetries = 3;
	static {
	    try {
	        String s = JOVALSystem.getProperty(JOVALSystem.PROP_SSH_READ_TIMEOUT);
	        if (s != null) {
	            readTimeout = Long.parseLong(s);
	        }
	    } catch (NumberFormatException e) {
	    }
	    try {
	        String s = JOVALSystem.getProperty(JOVALSystem.PROP_SSH_MAX_RETRIES);
	        if (s != null) {
	            execRetries = Integer.parseInt(s);
	        }
	    } catch (NumberFormatException e) {
	    }
	}

	public static Flavor flavorOf(IUnixSession session) {
	    Flavor flavor = UNKNOWN;
	    try {
		boolean success = false;
		String command = "uname -s";
		for (int attempt=0; !success; attempt++) {
		    try {
			IProcess p = session.createProcess(command);
			p.start();
			InputStream in = p.getInputStream();
			String osName = StreamTool.readLine(in, readTimeout);
			success = true;
			in.close();
			p.waitFor(0);
			for (Flavor f : values()) {
			    if (f.getOsName().equals(osName)) {
				flavor = f;
				break;
			    }
			}
		    } catch (EOFException e) {
			if (attempt > execRetries) {
			    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_SSH_PROCESS_RETRY, command, attempt);
			    throw e;
			} else {
			    JOVALSystem.getLogger().debug(JOVALMsg.STATUS_SSH_PROCESS_RETRY, command);
			}
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
