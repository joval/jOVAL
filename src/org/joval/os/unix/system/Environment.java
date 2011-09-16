// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A representation of an environment on a Unix machine.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environment implements IEnvironment {
    private Properties props;

    public Environment(ISession session) {
	props = new Properties();
	try {
	    IProcess p = session.createProcess("env");
	    p.start();
	    InputStream in = p.getInputStream();
	    props.load(in);
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Implement IEnvironment

    public String expand(String data) {
	if (data.indexOf('%') < 0) {
	    return data;
	}
	String originalData = data;
	Iterator <String>names = props.stringPropertyNames().iterator();
	while (names.hasNext()) {
	    String name = names.next();
	    String pattern = new StringBuffer("(?i)%").append(name).append('%').toString();
	    data = data.replaceAll(pattern, Matcher.quoteReplacement(props.getProperty(name)));
	}
	if (data.equals(originalData)) {
	    return data; // Some unexpandable pattern exists in there
	} else {
	    return expand(data); // Recurse, in case a variable includes another
	}
    }

    /**
     * Get an environment variable!
     */
    public String getenv(String var) {
	return props.getProperty(var.toUpperCase());
    }

    public Iterator<String> iterator() {
	return props.stringPropertyNames().iterator();
    }
}
