// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.di;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.joval.oval.OvalException;
import org.joval.oval.engine.Engine;
import org.joval.intf.plugin.IPlugin;

/**
 * Interface specification for a plugin to the Jovaldi command-line application.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IJovaldiPlugin extends IPlugin {
    String PROP_VERSION		= "version";
    String PROP_DESCRIPTION	= "description";
    String PROP_COPYRIGHT	= "copyright";
    String PROP_HELPTEXT	= "helpText";

    /**
     * A place where the plugin can maintain state across invocations.  Called immediately after the default constructor
     * is invoked.
     */
    public void setDataDirectory(File dataDir);

    /**
     * Configure the IJovaldiPlugin with arguments harvested from the commandline's -k option.
     */
    public boolean configure(String[] args, IJovaldiConfiguration jDIconfig);

    /**
     * Connect to any underlying resources required by the plugin (or its adapters).
     */
    public void connect();

    /**
     * Release any underlying resources.
     */
    public void disconnect();

    /**
     * Get the last error.  Generally, this will be a description of the reason that the confiure method might have
     * returned the value "false".
     */
    public String getLastError();

    /**
     * Get a property of the jovaldi plugin.  PROP_ keys should be supported, in the form of localized Strings.
     */
    public String getProperty(String name);
}
