// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import jsaf.provider.SessionFactory;

import org.joval.util.JOVALSystem;

/**
 * Local implementation of an IPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalPlugin extends BasePlugin {
    /**
     * Create a local plugin using the default data directory.
     */
    public LocalPlugin() throws IOException {
	this(JOVALSystem.getDataDirectory());
    }

    /**
     * Create a local plugin using the specified data directory.
     */
    public LocalPlugin(File dir) throws IOException {
	super(dir);
	SessionFactory sf = SessionFactory.newInstance(SessionFactory.DEFAULT_FACTORY, getClass().getClassLoader(), dir);
	session = sf.createSession();
	session.setLogger(logger);
    }

    // Implement IPlugin

    public void configure(Properties props) throws Exception {
	//no-op
    }
}
