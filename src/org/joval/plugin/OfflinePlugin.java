// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.di.IJovaldiPlugin;
import org.joval.intf.plugin.IAdapter;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A plugin with no adapters, exclusively for use with an existing SystemCharacteristics file.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OfflinePlugin implements IJovaldiPlugin {
    private static PropertyResourceBundle resources;
    static {
	try {
	    ClassLoader cl = OfflinePlugin.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("plugin.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("plugin.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("plugin.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Retrieve a message using its key.
     */
    protected static final String getMessage(String key, Object... arguments) {
        return MessageFormat.format(resources.getString(key), arguments);
    }

    protected List<IAdapter> adapters;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public OfflinePlugin() {
	adapters = new Vector<IAdapter>();
    }

    // Implement IJovaldiPlugin

    public boolean configure(Properties props) {
	return true;
    }

    public void setDataDirectory(File dir) {
    }

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public String getLastError() {
	return null;
    }

    // Implement IPlugin

    public void connect() throws OvalException {
	JOVALSystem.getLogger().info(JOVALMsg.STATUS_OFFLINE);
    }

    public void disconnect() {
    }

    public List<IAdapter> getAdapters() {
	return adapters;
    }

    public SystemInfoType getSystemInfo() {
	// In cases using the OfflinePlugin, the SystemInfoType will be supplied by the system-characteristics.xml file.
	return null;
    }
}
