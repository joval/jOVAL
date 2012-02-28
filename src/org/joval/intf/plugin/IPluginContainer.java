// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.io.File;
import java.util.Properties;

/**
 * Defines an interface for a plugin container, which is a utility class that manages an IPlugin, and configuration data that
 * is needed by that plugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPluginContainer {
    /**
     * The default filename for a plugin configuration.
     */
    String DEFAULT_FILE		= "config.properties";

    /**
     * An enumeration containing property keys that are used by the ContainerFactory.
     */
    enum FactoryProperty {
	NAME("name"),
	CLASSPATH("classpath"),
	MAIN("main"),
	FILENAME("plugin.properties");

	String value;

	public String value() {
	    return value;
	}

	FactoryProperty(String value) {
	    this.value = value;
	}
    }

    /**
     * Property specifying the name of the container.
     */
    String PROP_NAME		= "name";

    /**
     * Property specifying the classpath of the container.
     */
    String PROP_CLASSPATH	= "classpath";

    /**
     * Property specifying the main class of the container.
     */
    String PROP_MAIN		= "main";

    String PROP_DESCRIPTION	= "description";
    String PROP_VERSION		= "version";
    String PROP_COPYRIGHT	= "copyright";
    String PROP_HELPTEXT	= "helpText";

    /**
     * Get a property from the container, e.g., PROP_*.
     */
    public String getProperty(String key);

    /**
     * Retrieve a localized message from the container.
     */
    public String getMessage(String key, Object... arguments);

    /**
     * Get the container's plugin.
     */
    public IPlugin getPlugin();

    /**
     * If applicable, set the directory where the IPlugin can persist state information.  This must be set
     * prior to the configure method, or it will not be applied.
     */
    void setDataDirectory(File dir);

    /**
     * Configure the IPlugin using the specified Properties.
     */
    void configure(Properties props) throws Exception;
}
