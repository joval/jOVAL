// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.system;

import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.system.IEnvironment;
import org.joval.util.AbstractEnvironment;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * A representation of an environment on a Unix machine.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environment extends AbstractEnvironment {
    public Environment(IUnixSession session) {
	super();
	try {
	    for (String line : SafeCLI.multiLine("env", session, IUnixSession.Timeout.S)) {
		int ptr = line.indexOf("=");
		if (ptr > 0) {
		    props.setProperty(line.substring(0, ptr), line.substring(ptr+1));
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }
}
