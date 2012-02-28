// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.container;

import java.util.Properties;

import org.joval.plugin.LocalPlugin;

/**
 * Jovaldi continer for the LocalPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DefaultContainer extends AbstractContainer {
    public DefaultContainer() {
	super();
    }

    // Implement IPluginContainer

    @Override
    public void configure(Properties props) throws Exception {
	plugin = new LocalPlugin();
    }
}
