// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.net.UnknownHostException;
import java.util.Properties;

import org.joval.intf.discovery.ISessionFactory;
import org.joval.discovery.Local;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Implementation of an IPlugin for scanning the local host.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalPlugin extends BasePlugin {
    /**
     * Create a default plugin.
     */
    public LocalPlugin() {
	super();
	setTarget(ISessionFactory.LOCALHOST);
    }
}
