// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.joval.intf.util.IConfigurable;
import org.joval.intf.util.IProperty;

/**
 * A utility for configuring things based on their class hierarchy.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Configurator {
    private static final String CONFIG_RESOURCE	= "defaults.ini";
    private static IniFile config;

    static {
	config = new IniFile();
	try {
	    ClassLoader cl = Configurator.class.getClassLoader();
	    InputStream rsc = cl.getResourceAsStream(CONFIG_RESOURCE);
	    if (rsc == null) {
		JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, CONFIG_RESOURCE));
	    } else {
		config.load(rsc);
	    }
	} catch (IOException e) {
	    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Overlay sectioned system configuration parameters from a file.
     *
     * NB: new settings will only take effect for objects configured AFTER this call.
     */
    public static void addConfiguration(File f) throws IOException {
	JOVALMsg.getLogger().info(JOVALMsg.STATUS_CONFIG_OVERLAY, f.getPath());
	config.load(f);
    }

    /**
     * Configure an IConfigurable target in accordance with the jOVAL system configuration.
     */
    static void configure(IConfigurable target) {
	List<Class> visited = new ArrayList<Class>();
	for (Class clazz : target.getClass().getInterfaces()) {
	    configureInterface(clazz, target.getProperties(), visited, target.getClass().getName());
	}
	Class clazz = target.getClass().getSuperclass();
	while(clazz != null) {
	    for (Class intf : clazz.getInterfaces()) {
		if (!visited.contains(intf)) {
		    configureInterface(intf, target.getProperties(), visited, target.getClass().getName());
		}
	    }
	    clazz = clazz.getSuperclass();
	}
    }

    // Private

    /**
     * Recursively configure the class.
     */
    private static void configureInterface(Class clazz, IProperty prop, List<Class> visited, String classname) {
	//
	// First, configure all properties from this interface
	//
	try {
	    visited.add(clazz);
	    String section = clazz.getName();
	    for (String key : config.getSection(section)) {
		//
		// Since configuration happens from the bottom-up, make sure not to override any
		// properties that have already been set.
		//
		if (prop.getProperty(key) == null) {
		    String value = config.getProperty(section, key);
		    JOVALMsg.getLogger().debug(JOVALMsg.STATUS_CONFIG_SESSION, classname, key, value, clazz.getName());
		    prop.setProperty(key, config.getProperty(section, key));
		}
	    }
	} catch (NoSuchElementException e) {
	}

	//
	// Then, configure all super-interfaces
	//
	for (Class intf : clazz.getInterfaces()) {
	    if (!visited.contains(intf)) {
		configureInterface(intf, prop, visited, classname);
	    }
	}
    }
}
