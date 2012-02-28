// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.container;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.joval.identity.Credential;
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
public class RemoteContainer extends AbstractContainer {
    public RemoteContainer() {
	super();
    }

    // Implement IPluginContainer

    @Override
    public void configure(Properties props) throws Exception {
	super.configure(props);

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
}
