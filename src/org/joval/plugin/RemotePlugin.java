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
import org.joval.intf.identity.ICredentialStore;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Implementation of an IPlugin for remote scanning.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RemotePlugin extends BasePlugin {
    private static SessionFactory sessionFactory = new SessionFactory();

    /**
     * Set a location where the RemotePlugin class can store host discovery information.
     */
    public static void setDataDirectory(File dir) throws IOException {
	sessionFactory.setDataDirectory(dir);
    }

    /**
     * Set the ICredentialStore for the RemotePlugin class.
     */
    public static void setCredentialStore(ICredentialStore cs) {
	sessionFactory.setCredentialStore(cs);
    }

    /**
     * Add an SSH gateway through which the destination must be contacted.
     */
    public static void addRoute(String destination, String gateway) {
	sessionFactory.addRoute(destination, gateway);
    }

    protected String hostname;

    /**
     * Create a remote plugin.
     */
    public RemotePlugin(String hostname) {
	super();
	this.hostname = hostname;
    }

    // Overrides

    /**
     * @override
     */
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	sessionFactory.setLogger(logger);
    }

    // Implement IPlugin

    /**
     * Creation of the session is deferred until this point because it can be a blocking, time-consuming operation.  By
     * doing that as part of the connect routine, it happens inside of the IEngine's run method, which can be wrapped inside
     * a Thread.
     */
    public void connect() throws ConnectException {
	try {
	    session = sessionFactory.createSession(hostname);
	    super.connect();
	} catch (UnknownHostException e) {
	    throw new ConnectException(e.getMessage());
	}
    }
}
