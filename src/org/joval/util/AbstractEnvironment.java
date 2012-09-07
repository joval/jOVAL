// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;

import org.joval.intf.system.IEnvironment;

/**
 * A base-class for environments.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class AbstractEnvironment implements IEnvironment {
    protected Properties props;

    protected AbstractEnvironment() {
	props = new Properties();
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

    public String[] toArray() {
	ArrayList<String> list = new ArrayList<String>();
	for (String key : this) {
	    list.add(new StringBuffer(key).append("=").append(getenv(key)).toString());
	}
	return list.toArray(new String[list.size()]);
    }
}
