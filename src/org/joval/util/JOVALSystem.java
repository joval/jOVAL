// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Timer;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IProperty;

/**
 * This class is used to retrieve JOVAL-wide resources, like jOVAL properties and the jOVAL event system timer.
 * It is also used to configure properties that affect the behavior of sessions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JOVALSystem {
    /**
     * Property indicating the product name.
     */
    public static final String SYSTEM_PROP_PRODUCT = "productName";

    /**
     * Property indicating the product version.
     */
    public static final String SYSTEM_PROP_VERSION = "version";

    /**
     * Property indicating the product build date.
     */
    public static final String SYSTEM_PROP_BUILD_DATE = "build.date";

    private static final String SYSTEM_SECTION	= JOVALSystem.class.getName();
    private static final String CONFIG_RESOURCE	= "defaults.ini";

    private static Timer timer;
    private static IniFile config;

    static {
	timer = new Timer("jOVAL system timer", true);
	config = new IniFile();
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();

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
     * Retrieve the daemon Timer used for scheduled jOVAL tasks.
     */
    public static Timer getTimer() {
	return timer;
    }

    /**
     * Return a directory suitable for storing transient application data, like state information that may persist
     * between invocations.  This is either a directory called .jOVAL beneath the user's home directory, or on Windows,
     * it will be a directory named jOVAL in the appropriate AppData storage location.
     */
    public static File getDataDirectory() {
	File dataDir = null;
	if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1) {
	    String s = System.getenv("LOCALAPPDATA");
	    if (s == null) {
		s = System.getenv("APPDATA");
	    }
	    if (s != null) {
		File appDataDir = new File(s);
		dataDir = new File(appDataDir, "jOVAL");
	    }
	}
	if (dataDir == null) {
	    File homeDir = new File(System.getProperty("user.home"));
	    dataDir = new File(homeDir, ".jOVAL");
	}
	return dataDir;
    }

    /**
     * Overlay sectioned jOVAL system configuration parameters from a file.
     *
     * jOVAL is equipped with a default configuration that is loaded by this class's static initializer. When using
     * this method to override the defaults, make sure to do so prior to the creation of any session objects.
     */
    public static void addConfiguration(File f) throws IOException {
	JOVALMsg.getLogger().info(JOVALMsg.STATUS_CONFIG_OVERLAY, f.getPath());
	config.load(f);
    }

    /**
     * Configure a session in accordance with the jOVAL system configuration.
     */
    public static void configureSession(IBaseSession session) {
	List<Class> visited = new Vector<Class>();
	for (Class clazz : session.getClass().getInterfaces()) {
	    configureInterface(clazz, session.getProperties(), visited, session.getClass().getName());
	}
	Class clazz = session.getClass().getSuperclass();
	while(clazz != null) {
	    for (Class intf : clazz.getInterfaces()) {
		if (!visited.contains(intf)) {
		    configureInterface(intf, session.getProperties(), visited, session.getClass().getName());
		}
	    }
	    clazz = clazz.getSuperclass();
	}
    }

    /**
     * Retrieve an OVAL system property.
     *
     * @param key specify one of the PROP_* keys
     */
    public static String getSystemProperty(String key) {
	try {
	    return config.getProperty(SYSTEM_SECTION, key);
	} catch (NoSuchElementException e) {
	}
	return null;
    }

    // Private

    /**
     * Recursively configure the class.
     */
    private static void configureInterface(Class clazz, IProperty prop, List<Class> visited, String sessionClassname) {
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
		    JOVALMsg.getLogger().debug(JOVALMsg.STATUS_CONFIG_SESSION, sessionClassname, key, value, clazz.getName());
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
		configureInterface(intf, prop, visited, sessionClassname);
	    }
	}

    }
}
