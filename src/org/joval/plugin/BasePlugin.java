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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import jsaf.intf.system.ISession;
import jsaf.provider.SessionFactory;

import ch.qos.cal10n.IMessageConveyor;
import org.slf4j.cal10n.LocLogger;

import org.openscap.sce.results.SceResultsType;
import org.openscap.sce.xccdf.ScriptDataType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.oval.IBatch;
import org.joval.intf.scap.oval.IProvider;
import org.joval.scap.oval.Batch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.sysinfo.SysinfoFactory;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * The abstract base class for all functional jovaldi plug-ins.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BasePlugin implements IPlugin, IProvider, IBatch {
    //
    // Extend the JOVALMsg logging enumeration with the jsaf.Message logging enumeration. This makes it possible
    // for the LocalPlugin to route jSAF log messages to a JOVALMsg-provided logger.
    //
    static {
	for (Map.Entry<Class<? extends Enum<?>>, IMessageConveyor> entry : jsaf.Message.getConveyors()) {
	    JOVALMsg.extend(entry.getKey(), entry.getValue());
	}
    }

    private static final String ADAPTERS_RESOURCE = "adapters.txt";
    private static final String BASENAME = "plugin";

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

    private PropertyResourceBundle resources;
    private Map<Class, IAdapter> adapters;
    private Collection<Class> notapplicable;
    private Collection<IBatch> pending;

    protected LocLogger logger;
    protected ISession session;

    /**
     * Initializes the logger and loads message resources from plugin[_locale].properties
     */
    protected BasePlugin() throws IOException {
	logger = JOVALMsg.getLogger();
	ClassLoader cl = LocalPlugin.class.getClassLoader();
	Locale locale = Locale.getDefault();
	URL url = cl.getResource(BASENAME + ".resources_" + locale.toString() + ".properties");
	if (url == null) {
	    url = cl.getResource(BASENAME + ".resources_" + locale.getLanguage() + ".properties");
	}
	if (url == null) {
	    url = cl.getResource(BASENAME + ".resources.properties");
	}
	resources = new PropertyResourceBundle(url.openStream());
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
	if (session != null) {
	    session.setLogger(logger);
	}
    }

    // Implement IPlugin

    public abstract void configure(Properties props) throws Exception;

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    public boolean isConnected() {
	return session.isConnected();
    }

    public boolean connect() {
	if (session.isConnected()) {
	    loadAdapters();
	    return true;
	} else if (session.connect()) {
	    loadAdapters();
	    return true;
	}
	return false;
    }

    public void disconnect() {
	session.disconnect();
    }

    public void dispose() {
	if (adapters != null) {
	    adapters.clear();
	    adapters = null;
	}
	if (notapplicable != null) {
	    notapplicable.clear();
	    notapplicable = null;
	}
	if (session != null) {
	    session.dispose();
	    session = null;
	}
    }

    public ISession getSession() {
	return session;
    }

    public IProvider getOvalProvider() {
	return this;
    }

    public SystemInfoType getSystemInfo() throws OvalException {
	return SysinfoFactory.createSystemInfo(session);
    }

    public Object getService(String name) {
	throw new UnsupportedOperationException("getService");
    }

    // Implement IProvider

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (adapters.containsKey(obj.getClass())) {
	    return adapters.get(obj.getClass()).getItems(obj, rc);
	} else if (notapplicable.contains(obj.getClass())) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.STATUS_NOT_APPLICABLE, obj.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_APPLICABLE);
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_ADAPTER_MISSING, obj.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
    }

    // Implement IBatch

    public boolean queue(IRequest request) {
	Class clazz = request.getObject().getClass();
	if (adapters.containsKey(clazz)) {
	    IAdapter adapter = adapters.get(clazz);
	    if (!notapplicable.contains(clazz) && adapter instanceof IBatch) {
		IBatch batch = (IBatch)adapter;
		if (batch.queue(request)) {
		    if (pending == null) {
			pending = new HashSet<IBatch>();
		    }
		    if (!pending.contains(batch)) {
			pending.add(batch);
		    }
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Iterate through and execute any pending batches.
     */
    public Collection<IResult> exec() {
	Collection<IResult> results = new ArrayList<IResult>();
	if (pending == null) {
	    return results;
	}
	for (IBatch batch : pending) {
	    results.addAll(batch.exec());
	}
	pending = null;
	return results;
    }

    // Private

    /**
     * Idempotent
     */
    private void loadAdapters() {
	if (notapplicable == null) {
	    notapplicable = new HashSet<Class>();
	}
	if (adapters == null) {
	    adapters = new HashMap<Class, IAdapter>();
	    Collection<IAdapter> coll = getAdapters();
	    if (coll != null) {
		adapters = new HashMap<Class, IAdapter>();
		for (IAdapter adapter : coll) {
		    for (Class clazz : adapter.init(session, notapplicable)) {
			adapters.put(clazz, adapter);
		    }
		}
	    }
	}
    }
}
