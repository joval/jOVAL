// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.net.UnknownHostException;
import java.util.Properties;

import org.joval.discovery.Local;
import org.joval.intf.system.ISession;

/**
 * Implementation of an IPlugin for scanning the local host.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalPlugin extends BasePlugin {
    public LocalPlugin() {
	super();
    }

    @Override
    public void configure(Properties props) {
	session = Local.createSession(dir);
    }
}
