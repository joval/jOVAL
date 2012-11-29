// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.util.AbstractEnvironment;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * A representation of the Windows SYSTEM environment, retrieved from the set command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environment extends AbstractEnvironment {
    public Environment(IBaseSession session) throws Exception {
	super();
	String lastKey = null;
	for (String line : SafeCLI.multiLine("set", session, IBaseSession.Timeout.M)) {
	    int ptr = line.indexOf("=");
	    if (ptr > 0) {
		String key = line.substring(0,ptr);
		lastKey = key;
		String val = line.substring(ptr+1);
		props.setProperty(key.toUpperCase(), val);
	    } else if (lastKey != null) {
		String val = new StringBuffer(props.getProperty(lastKey.toUpperCase())).append(line).toString();
		props.setProperty(lastKey.toUpperCase(), val);
	    }
	}
    }

    /**
     * Determine whether this environment was sourced from a 64-bit Windows OS.
     */
    public boolean is64bit() {
	if (getenv(IWindowsSession.ENV_ARCH).indexOf("64") != -1) {
	    return true;
	} else {
	    String ar32 = getenv(IWindowsSession.ENV_AR32);
	    if (ar32 == null) {
		return false;
	    } else if (ar32.indexOf("64") != -1) {
		return true;
	    }
	}
	return false;
    }
}
