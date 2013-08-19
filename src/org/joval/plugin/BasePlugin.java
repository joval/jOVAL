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
import java.util.Arrays;
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
    private Statistics statistics;
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
	if (statistics != null) {
	    statistics.dumpTrace(logger);
	    statistics = null;
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
	    try {
		statistics.start(obj);
		return adapters.get(obj.getClass()).getItems(obj, rc);
	    } finally {
		statistics.stop(obj);
	    }
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
		    statistics.queue(batch, request);
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
	    try {
		statistics.start(batch);
		results.addAll(batch.exec());
	    } finally {
		statistics.stop(batch);
	    }
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
	    statistics = new Statistics();
	    Collection<IAdapter> coll = getAdapters();
	    if (coll != null) {
		adapters = new HashMap<Class, IAdapter>();
		for (IAdapter adapter : coll) {
		    for (Class clazz : adapter.init(session, notapplicable)) {
			adapters.put(clazz, adapter);
			statistics.add(clazz);
		    }
		}
	    }
	}
    }

    static class Statistics {
	private Map<Class, TimeKeeper> timekeepers;
	private Map<Class, Class> batchAssoc;

	Statistics() {
	    timekeepers = new HashMap<Class, TimeKeeper>();
	    batchAssoc = new HashMap<Class, Class>();
	}

	void add(Class clazz) {
	    timekeepers.put(clazz, new TimeKeeper());
	}

	void start(ObjectType object) {
	    TimeKeeper tk = timekeepers.get(object.getClass());
	    if (tk != null) {
		tk.start(object.getId());
	    }
	}

	void stop(ObjectType object) {
	    TimeKeeper tk = timekeepers.get(object.getClass());
	    if (tk != null) {
		tk.stop(object.getId());
	    }
	}

	void queue(IBatch batch, IRequest request) {
	    ObjectType object = request.getObject();
	    if (!batchAssoc.containsKey(batch.getClass())) {
		batchAssoc.put(batch.getClass(), object.getClass());
	    }
	    TimeKeeper tk = timekeepers.get(object.getClass());
	    if (tk != null) {
		tk.queue(object);
	    }
	}

	void start(IBatch batch) {
	    TimeKeeper tk = timekeepers.get(batchAssoc.get(batch.getClass()));
	    if (tk != null) {
		tk.startBatch();
	    }
	}

	void stop(IBatch batch) {
	    TimeKeeper tk = timekeepers.get(batchAssoc.get(batch.getClass()));
	    if (tk != null) {
		tk.stopBatch();
	    }
	}

	void dumpTrace(LocLogger logger) {
	    logger.trace("Adapter Statistics:");
	    for (Map.Entry<Class, TimeKeeper> entry : timekeepers.entrySet()) {
		TimeKeeper tk = entry.getValue();
		if (tk.size() > 0) {
		    logger.trace("  Adapter " + entry.getKey().getName());
		    entry.getValue().dumpTrace(logger);
		}
	    }
	}
    }

    static class TimeKeeper {
	private Map<String, Duration> objects;
	private Collection<String> batched;
	private Duration batchDuration;

	TimeKeeper() {
	    objects = new HashMap<String, Duration>();
	}

	void start(String objectId) {
	    objects.put(objectId, new Duration(true));
	}

	void stop(String objectId) {
	    Duration d = objects.get(objectId);
	    if (d != null) {
		d.stop();
	    }
	}

	void queue(ObjectType object) {
	    if (batched == null) {
		batched = new ArrayList<String>();
		batchDuration = new Duration(false);
	    }
	    batched.add(object.getId());
	}

	void startBatch() {
	    batchDuration.start();
	}

	void stopBatch() {
	    batchDuration.stop();
	}

	int size() {
	    return objects.size() + (batched == null? 0 : batched.size());
	}

	void dumpTrace(LocLogger logger) {
	    if (batched != null && batchDuration.isValid()) {
		logger.trace("    Batch duration: " + batchDuration.duration() + "ms");
		if (batched.size() > 0) {
		    logger.trace("    Batched Objects:");
		    for (String id : batched) {
			logger.trace("      " + id);
		    }
		}
	    }
	    if (objects.size() > 0) {
		logger.trace("    Objects:");
		for (Map.Entry<String, Duration> entry : objects.entrySet()) {
		    logger.trace("      " + entry.getKey() + ": " + entry.getValue().duration() + "ms");
		}
	    }
	}
    }

    static class Duration {
	private long start, stop;

	Duration(boolean autostart) {
	    stop = 0;
	    if (autostart) {
		start();
	    } else {
		start = 0;
	    }
	}

	void start() {
	    start = System.currentTimeMillis();
	}

	void stop() {
	    stop = System.currentTimeMillis();
	}

	boolean isValid() {
	    return stop > start;
	}

	long duration() {
	    return stop - start;
	}
    }
}
