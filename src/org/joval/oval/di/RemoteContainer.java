// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import org.joval.identity.SimpleCredentialStore;
import org.joval.intf.plugin.IPlugin;
import org.joval.plugin.RemotePlugin;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Jovaldi continer for the RemotePlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RemoteContainer implements IPluginContainer {
    private static PropertyResourceBundle resources;
    static {
	try {
	    ClassLoader cl = RemoteContainer.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("plugin.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("plugin.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("plugin.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private IPlugin plugin;
    private File dir;

    public RemoteContainer() {
    }

    // Implement IPluginContainer

    public void setDataDirectory(File dir) {
	this.dir = dir;
    }

    public void configure(Properties props) throws Exception {
	if (props.getProperty("config.file") == null) {
	    throw new Exception("Missing configuration file: " + ExecutionState.DEFAULT_CONFIG);
	} if (props.getProperty("hostname") == null) {
	    throw new Exception("Missing property: hostname");
	}
	RemotePlugin.setCredentialStore(new SimpleCredentialStore(props));
	if (dir != null) {
	    RemotePlugin.setDataDirectory(dir);
	}
	plugin = new RemotePlugin(props.getProperty("hostname"));
    }

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public IPlugin getPlugin() {
	return plugin;
    }
}
