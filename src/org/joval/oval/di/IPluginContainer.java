// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.File;
import java.util.Properties;

import org.joval.intf.plugin.IPlugin;

/**
 * An interface for the plugin harness.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPluginContainer {
    /**
     * The default filename for a plugin configuration.
     */
    String DEFAULT_FILE = "config.properties";

    /**
     * The property indicating the plugin filename.  This is a required property.
     */
    String PROP_CONFIGFILE = "config.file";

    String PROP_DESCRIPTION = "description";
    String PROP_VERSION = "version";
    String PROP_COPYRIGHT = "copyright";
    String PROP_HELPTEXT = "helpText";

    void setDataDirectory(File dir);

    void configure(Properties props) throws Exception;

    public String getProperty(String key);

    public IPlugin getPlugin();
}
