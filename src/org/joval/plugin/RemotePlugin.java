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
    public static final String PROP_GATEWAY_HOST		= "gw.host";
    public static final String PROP_GATEWAY_USER		= "gw.user";
    public static final String PROP_GATEWAY_PASSWORD		= "gw.pass";
    public static final String PROP_GATEWAY_PRIVATE_KEY		= "gw.keyFile";
    public static final String PROP_GATEWAY_KEY_PASSPHRASE	= "gw.keyPass";

    private SessionFactory sessionFactory = new SessionFactory();
    private ICredentialStore cs;

    protected String hostname;

    /**
     * Set the target hostname (i.e., domain name or address).
     */
    public void setHostname(String hostname) {
	this.hostname = hostname;
    }

    /**
     * Set the ICredentialStore for the RemotePlugin class.
     */
    public void setCredentialStore(ICredentialStore cs) {
	this.cs = cs;
	sessionFactory.setCredentialStore(cs);
    }

    /**
     * Add an SSH gateway through which the destination must be contacted.
     */
    public void addRoute(String destination, String gateway) {
	sessionFactory.addRoute(destination, gateway);
    }

    // Overrides

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	sessionFactory.setLogger(logger);
    }

    // Implement IPlugin

    @Override
    public IBaseSession getSession() throws IOException {
	if (session == null) {
	    session = sessionFactory.createSession(hostname);
	}
	return session;
    }

    /**
     * If a hostname and ICredentialStore have not been previously set, a default SimpleCredentialStore will be created
     * and configured using the specified Properties, and the target hostname will also be condifured from the Properties.
     */
    @Override
    public void configure(Properties props) throws Exception {
	super.configure(props);

	if (hostname == null) {
	    hostname = props.getProperty(SimpleCredentialStore.PROP_HOSTNAME);
	}

	//
	// If a gateway is defined, provision the SessionFactory with the route.
	//
	String gateway = props.getProperty(PROP_GATEWAY_HOST);
	if (gateway != null) {
	    addRoute(hostname, gateway);
	}

	//
	// If no credential store has been defined, create a simple one, and read login information from the properties.
	//
	if (cs == null) {
	    //
	    // Provision the CredentialStore with the configuration, which will validate that all the necessary
	    // information has been provided.
	    //
	    SimpleCredentialStore scs = new SimpleCredentialStore();
	    scs.add(props);

	    //
	    // If a gateway is defined, provision the SimpleCredentialStore with its username/password.
	    //
	    if (gateway != null) {
		Properties gwProps = new Properties();
		gwProps.setProperty(SimpleCredentialStore.PROP_HOSTNAME, gateway);
		gwProps.setProperty(SimpleCredentialStore.PROP_USERNAME, props.getProperty(PROP_GATEWAY_USER));
		String temp = null;
		if ((temp = props.getProperty(PROP_GATEWAY_PASSWORD)) != null) {
		    gwProps.setProperty(SimpleCredentialStore.PROP_PASSWORD, temp);
		}
		if ((temp = props.getProperty(PROP_GATEWAY_PRIVATE_KEY)) != null) {
		    gwProps.setProperty(SimpleCredentialStore.PROP_PRIVATE_KEY, temp);
		}
		if ((temp = props.getProperty(PROP_GATEWAY_KEY_PASSPHRASE)) != null) {
		    gwProps.setProperty(SimpleCredentialStore.PROP_PASSPHRASE, temp);
		}
		scs.add(gwProps);
	    }

	    setCredentialStore(scs);
	}
    }

    @Override
    public void setDataDirectory(File dir) throws IOException {
	sessionFactory.setDataDirectory(dir);
    }
}
