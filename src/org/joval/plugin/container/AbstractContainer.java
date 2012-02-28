// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.plugin.IPluginContainer;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Abstract container for a generic IPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
abstract class AbstractContainer implements IPluginContainer {
    PropertyResourceBundle resources;
    IPlugin plugin;
    File dir;

    /**
     * Loads localized resources for the plugin using the default basename, "plugin".
     */
    AbstractContainer() {
	this("plugin");
    }

    /**
     * Loads localized resources for the plugin using a customized basename.
     */
    AbstractContainer(String basename) {
	try {
	    ClassLoader cl = AbstractContainer.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource(basename + ".resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource(basename + ".resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource(basename + ".resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Implement IPluginContainer

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    public IPlugin getPlugin() {
	return plugin;
    }

    public void setDataDirectory(File dir) {
	this.dir = dir;
    }

    /**
     * Subclasses should override this method.
     */
    public void configure(Properties props) throws Exception {
	if (props == null) {
	    throw new Exception(getMessage("err.configMissing", DEFAULT_FILE));
	}
    }
}
