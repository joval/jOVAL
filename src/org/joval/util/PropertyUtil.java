// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Iterator;
import java.util.Properties;

import org.joval.intf.util.IProperty;

/**
 * Convenience class for getting typed data from an IProperty.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PropertyUtil implements IProperty {
    private IProperty prop;

    /**
     * Create a new PropertyUtil for accessing the given IProperty.
     */
    public PropertyUtil(IProperty prop) {
	this.prop = prop;
    }

    /**
     * Create a new PropertyUtil based on a java.util.Properties.
     */
    public PropertyUtil(Properties props) {
	this.prop = new PropertiesProperty(props);
    }

    /**
     * Create an empty PropertyUtil.
     */
    public PropertyUtil() {
	this(new Properties());
    }

    // Implement IProperty

    public void setProperty(String key, String value) {
	prop.setProperty(key, value);
    }

    public int getIntProperty(String key) {
	return prop.getIntProperty(key);
    }

    public long getLongProperty(String key) {
	return prop.getLongProperty(key);
    }

    public boolean getBooleanProperty(String key) {
	return prop.getBooleanProperty(key);
    }

    public String getProperty(String key) {
	return prop.getProperty(key);
    }

    public Iterator<String> iterator() {
	return prop.iterator();
    }

    public Properties toProperties() {
	return prop.toProperties();
    }

    // Private

    private class PropertiesProperty implements IProperty {
	Properties props;

	PropertiesProperty(Properties props) {
	    this.props = props;
	}

	// Implement IProperty

	public String getProperty(String key) {
	    return props.getProperty(key);
	}

	public void setProperty(String key, String val) {
	    if (val == null) {
		props.remove(key);
	    } else {
		props.setProperty(key, val);
	    }
	}

	public Iterator<String> iterator() {
	    return props.stringPropertyNames().iterator();
	}

	public long getLongProperty(String key) {
	    long l = 0;
	    try {
		String s = prop.getProperty(key);
		if (s != null) {
		    l = Long.parseLong(s);
		}
	    } catch (NumberFormatException e) {
	    }
	    return l;
	}

	public int getIntProperty(String key) {
	    int i = 0;
	    try {
		String s = prop.getProperty(key);
		if (s != null) {
		    i = Integer.parseInt(s);
		}
	    } catch (NumberFormatException e) {
	    }
	    return i;
	}

	public boolean getBooleanProperty(String key) {
	    return "true".equalsIgnoreCase(prop.getProperty(key));
	}

	public Properties toProperties() {
	    return props;
	}
    }
}
