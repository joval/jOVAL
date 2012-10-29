// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.slf4j.cal10n.LocLogger;

import org.joval.discovery.Local;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.system.ISession;
import org.joval.intf.system.ISessionProvider;
import org.joval.intf.util.ILoggable;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.sysinfo.SysinfoFactory;
import org.joval.util.JOVALMsg;

/**
 * The abstract base class for all functional jovaldi plug-ins.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalPlugin implements IPlugin, ILoggable, ISessionProvider {
    private static final String ADAPTERS_RESOURCE = "adapters.txt";

    /**
     * Get a list of all the IAdapters that are bundled with the plugin.
     */
    private static Collection<IAdapter> getAdapters() {
	Collection<IAdapter> adapters = new HashSet<IAdapter>();
	try {
	    ClassLoader cl = LocalPlugin.class.getClassLoader();
	    InputStream rsc = cl.getResourceAsStream(ADAPTERS_RESOURCE);
	    if (rsc == null) {
		JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, ADAPTERS_RESOURCE));
	    } else {
		BufferedReader reader = new BufferedReader(new InputStreamReader(rsc));
		String line = null;
		while ((line = reader.readLine()) != null) {
		    if (!line.startsWith("#")) {
			try {
			    Object obj = Class.forName(line).newInstance();
			    if (obj instanceof IAdapter) {
				adapters.add((IAdapter)obj);
			    }
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (ClassNotFoundException e) {
			}
		    }
		}
	    }
	} catch (IOException e) {
	    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return adapters;
    }

    private LocLogger logger;
    private ISession session;
    private PropertyResourceBundle resources;
    private File dir;
    private Hashtable<Class, IAdapter> adapters;

    /**
     * Loads localized resources for the plugin using the default basename, "plugin".
     */
    public LocalPlugin() {
	this("plugin");
    }

    /**
     * Loads localized resources for the plugin using a customized basename.
     */
    public LocalPlugin(String basename) {
	logger = JOVALMsg.getLogger();
	try {
	    ClassLoader cl = LocalPlugin.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource(basename + ".resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource(basename + ".resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource(basename + ".resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
	session.setLogger(logger);
    }

    // Implement IPlugin

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    public void setDataDirectory(File dir) throws IOException {
	this.dir = dir;
    }

    public void configure(Properties props) {
	session = Local.createSession(dir);
	loadAdapters();
    }

    public boolean isConnected() {
	return session.isConnected();
    }

    public boolean connect() {
	return session.connect();
    }

    public void disconnect() {
	session.disconnect();
    }

    public void dispose() {
	adapters.clear();
	session.dispose();
    }

    public SystemInfoType getSystemInfo() throws OvalException {
	return SysinfoFactory.createSystemInfo(session);
    }

    public long getTime() {
	try {
	    return session.getTime();
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return System.currentTimeMillis();
	}
    }

    // Implement IProvider

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (adapters.containsKey(obj.getClass())) {
	    return adapters.get(obj.getClass()).getItems(obj, rc);
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_ADAPTER_MISSING, obj.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
    }

    // Implement ISessionProvider

    public ISession getSession() {
	return session;
    }

    // Private

    /**
     * Idempotent
     */
    private void loadAdapters() {
	if (adapters == null) {
	    adapters = new Hashtable<Class, IAdapter>();
	    Collection<IAdapter> coll = getAdapters();
	    if (coll != null) {
		adapters = new Hashtable<Class, IAdapter>();
		for (IAdapter adapter : coll) {
		    for (Class clazz : adapter.init(session)) {
			adapters.put(clazz, adapter);
		    }
		}
	    }
	}
    }
}
