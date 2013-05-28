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
import javax.xml.bind.JAXBElement;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.slf4j.cal10n.LocLogger;

import jsaf.intf.util.IProperty;

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

    private static final String SYSPROPS_RESOURCE = "joval.system.properties";

    private static Timer timer;
    private static Properties sysProps;
    static {
	timer = new Timer("jOVAL system timer", true);
	sysProps = new Properties();
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();

	    InputStream rsc = cl.getResourceAsStream(SYSPROPS_RESOURCE);
	    if (rsc == null) {
		JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, SYSPROPS_RESOURCE));
	    } else {
		sysProps.load(rsc);
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
     * Supported versions of XSL/XPATH.
     */
    public enum XSLVersion {
	V1("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"),
	V2("net.sf.saxon.TransformerFactoryImpl");

	private TransformerFactory factory;
	private String className;

	private XSLVersion(String className) {
	    this.className = className;
	    factory = null;
	}

	public TransformerFactory getFactory() throws TransformerFactoryConfigurationError {
	    if (factory == null) {
		factory = TransformerFactory.newInstance(className, JOVALSystem.class.getClassLoader());
	    }
	    return factory;
	}
    }

    /**
     * Return a directory suitable for storing transient application data, like state information that may persist
     * between invocations.  This is either a directory called .jOVAL beneath the user's home directory, or on Windows,
     * it will be a directory named jOVAL in the appropriate AppData storage location.
     */
    public static synchronized File getDataDirectory() {
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
	if (!dataDir.exists()) {
	    dataDir.mkdirs();
	}
	return dataDir;
    }

    /**
     * Retrieve an OVAL system property.
     *
     * @param key specify one of the SYSTEM_PROP_* keys
     */
    public static String getSystemProperty(String key) {
	return sysProps.getProperty(key);
    }

    public static void setSystemProperty(String key, String value) {
	sysProps.setProperty(key, value);
    }
}
