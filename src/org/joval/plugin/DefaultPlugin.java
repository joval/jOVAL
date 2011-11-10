// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.util.Properties;

import org.joval.discovery.Local;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;

/**
 * Implementation of an IJovaldiPlugin for scanning the local host.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DefaultPlugin extends OnlinePlugin {
    /**
     * Create a default plugin.
     */
    public DefaultPlugin() {
	super();
    }

    // Implement IJovaldiPlugin

    /**
     * @override
     */
    public boolean configure(Properties props) {
	session = Local.getSession();
	return true;
    }
}
