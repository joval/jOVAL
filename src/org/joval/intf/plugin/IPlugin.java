// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.cal10n.LocLogger;

import jsaf.intf.system.ISession;
import jsaf.intf.util.ILoggable;

import scap.oval.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.scap.oval.IProvider;
import org.joval.plugin.PluginConfigurationException;
import org.joval.scap.oval.OvalException;

/**
 * Defines an interface for an OVAL engine plugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPlugin extends ILoggable {
    /**
     * Default filename for plugin configuration.
     */
    String DEFAULT_FILE = "config.properties";

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
     * Configure the IPlugin using the specified Properties.
     */
    void configure(Properties props) throws Exception;

    /**
     * Connect the plugin to the target.
     */
    boolean connect();

    /**
     * Returns whether or not the plugin is connected to the target.
     */
    boolean isConnected();

    /**
     * Disconnect the plugin from the target.
     */
    void disconnect();

    /**
     * Returns the jSAF session used to interact with the target machine.
     */
    ISession getSession();

    /**
     * Get the org.joval.intf.oval.IProvider
     */
    IProvider getOvalProvider();

    /**
     * Get OVAL SystemInfoType information about the host.
     */
    SystemInfoType getSystemInfo() throws OvalException;

    /**
     * When you're completely finished using the plugin, call this method to clean up caches and other resources.
     */
    void dispose();
}
