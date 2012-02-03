// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import org.joval.identity.Credential;
import org.joval.identity.SimpleCredentialStore;
import org.joval.intf.plugin.IPlugin;
import org.joval.plugin.RemotePlugin;
import org.joval.ssh.system.SshSession;
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
	    throw new Exception("Missing configuration file: " + DEFAULT_FILE);
	}

	//
	// Provision the CredentialStore with the configuration, which will validate that all the necessary
	// information has been provided.
	//
	SimpleCredentialStore scs = new SimpleCredentialStore();
	scs.add(props);

	//
	// If a gateway is defined, provision the SessionFactory with the route, and the CredentialStore with its
	// username/password.
	//
	String gateway = props.getProperty("gw.host");
	if (gateway != null) {
	    Properties gwProps = new Properties();
	    gwProps.setProperty(SimpleCredentialStore.PROP_HOSTNAME, gateway);
	    gwProps.setProperty(SimpleCredentialStore.PROP_USERNAME, props.getProperty("gw.user"));
	    String temp = null;
	    if ((temp = props.getProperty("gw.pass")) != null) {
		gwProps.setProperty(SimpleCredentialStore.PROP_PASSWORD, temp);
	    }
	    if ((temp = props.getProperty("gw.keyFile")) != null) {
		gwProps.setProperty(SimpleCredentialStore.PROP_PRIVATE_KEY, temp);
	    }
	    if ((temp = props.getProperty("gw.keyPass")) != null) {
		gwProps.setProperty(SimpleCredentialStore.PROP_PASSPHRASE, temp);
	    }
	    scs.add(gwProps);

	    String destination = props.getProperty(SimpleCredentialStore.PROP_HOSTNAME);
	    RemotePlugin.addRoute(destination, gateway);
	}

	RemotePlugin.setCredentialStore(scs);
	if (dir != null) {
	    RemotePlugin.setDataDirectory(dir);
	}
	plugin = new RemotePlugin(props.getProperty(SimpleCredentialStore.PROP_HOSTNAME));
    }

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public IPlugin getPlugin() {
	return plugin;
    }
}
