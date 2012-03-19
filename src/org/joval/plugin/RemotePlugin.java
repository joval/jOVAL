// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.cal10n.LocLogger;

import org.joval.discovery.SessionFactory;
import org.joval.identity.Credential;
import org.joval.identity.SimpleCredentialStore;
import org.joval.intf.identity.ICredentialStore;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.system.IBaseSession;
import org.joval.util.JOVALMsg;

/**
 * Implementation of an IPlugin for remote scanning.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RemotePlugin extends BasePlugin {
    private SessionFactory sessionFactory = new SessionFactory();

    protected String hostname;

    // Overrides

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	sessionFactory.setLogger(logger);
    }

    // Implement IPlugin

    @Override
    public IBaseSession getSession() {
	try {
	    if (session == null) {
		session = sessionFactory.createSession(hostname);
	    }
	} catch (ConnectException e) {
	    throw new RuntimeException(e.getMessage());
	} catch (UnknownHostException e) {
	    throw new RuntimeException(e.getMessage());
	}
	return session;
    }

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
	    addRoute(destination, gateway);
	}

	setCredentialStore(scs);
	if (dir != null) {
	    setDataDirectory(dir);
	}
	hostname = props.getProperty(SimpleCredentialStore.PROP_HOSTNAME);
    }

    /**
     * Set a location where the RemotePlugin class can store host discovery information.
     */
    public void setDataDirectory(File dir) throws IOException {
	sessionFactory.setDataDirectory(dir);
    }

    /**
     * Set the ICredentialStore for the RemotePlugin class.
     */
    public void setCredentialStore(ICredentialStore cs) {
	sessionFactory.setCredentialStore(cs);
    }

    /**
     * Add an SSH gateway through which the destination must be contacted.
     */
    public void addRoute(String destination, String gateway) {
	sessionFactory.addRoute(destination, gateway);
    }
}
