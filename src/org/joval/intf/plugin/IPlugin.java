// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.List;
import java.util.Properties;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.oval.OvalException;
import org.joval.oval.engine.Engine;

/**
 * The interface for defining a plugin for the Oval Engine.  The plugin is a container for IAdapters, produces the
 * SystemInfoType information, and also returns the family type of the host.
 *
 * In order to do its job, the IPlugin should first determine the host type, and then register adapters based on its
 * assessment.  For example, it makes little sense to register Windows adapters for a Linux host.  Similarly, the plugin
 * must know the type of host in order to properly respond to the getFamily call.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPlugin {
    /**
     * List the IAdapters provided by this host.
     */
    public List<IAdapter> getAdapters();

    /**
     * Return SystemInfoType data about the host.
     */
    public SystemInfoType getSystemInfo();

    /**
     * Return the FamilyEnumeration member against which the host should be tested for FamilyTest applicability.
     */
    public FamilyEnumeration getFamily();
}
