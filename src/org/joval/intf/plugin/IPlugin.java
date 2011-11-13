// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.Collection;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.identity.ICredential;
import org.joval.oval.OvalException;

/**
 * The interface for defining a plugin for an IEngine.  The plugin is a container for IAdapters and it also produces the
 * SystemInfoType information.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPlugin {
    /**
     * Connect the plugin to whatever it's going to be used to scan.
     *
     * @throws OvalException if the plugin failed to establish the connection.
     */
    public void connect() throws OvalException;

    /**
     * Release any underlying resources.
     */
    public void disconnect();

    /**
     * List the IAdapters provided by this host.
     */
    public Collection<IAdapter> getAdapters();

    /**
     * Collect SystemInfoType information from the host.
     */
    public SystemInfoType getSystemInfo();
}
