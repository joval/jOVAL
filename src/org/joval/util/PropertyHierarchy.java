// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.joval.util.StringTools;

/**
 * A class that represents a hierarchy of properties. Getting a property of a path will return the
 * value found at the nearest ancestor to that path with the given key.
 *
 * For example, if you set foo=bar at /my/path
 * Then getProperty(/my/path/under, foo) will return bar.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PropertyHierarchy {
    private Hashtable<String, Properties> table;
    private String delimiter;

    public PropertyHierarchy(String delimiter) {
	this.delimiter = delimiter;
	table = new Hashtable<String, Properties>();
    }

    /**
     * Get a hierarchical property value.
     *
     * @param path denotes path beneath the root node.
     */
    public String getProperty(String path, String key) {
	String solution = null;
	List<String> parts = StringTools.toList(StringTools.tokenize(path, delimiter));
	StringBuffer sb = new StringBuffer();
	for (String part : parts) {
	    if (sb.length() > 0) {
		sb.append(delimiter);
	    }
	    sb.append(part);
	    if (table.containsKey(sb.toString())) {
		Properties props = table.get(sb.toString());
		if (props.containsKey(key)) {
		    solution = props.getProperty(key);
		}
	    }
	}
	return solution;
    }

    /**
     * Set a hierarchical property value.
     *
     * @param path denotes path beneath the root node.
     */
    public void setProperty(String path, String key, String value) {
	Properties props = null;
	if (table.containsKey(path)) {
	    props = table.get(path);
	} else {
	    props = new Properties();
	    table.put(path, props);
	}
	props.put(key, value);
    }
}
