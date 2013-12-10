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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.TreeSet;

import jsaf.intf.system.ISession;
import jsaf.provider.SessionFactory;
import jsaf.util.StringTools;

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

    private static HashSet<Class<IAdapter>> ADAPTER_CLASSES = new HashSet<Class<IAdapter>>();
    static {
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
			    Class<?> clazz = Class.forName(line);
			    for (Class<?> iface : getInterfaces(clazz)) {
				if ("org.joval.intf.plugin.IAdapter".equals(iface.getName())) {
				    @SuppressWarnings("unchecked")
				    Class<IAdapter> adapterClass = (Class<IAdapter>)clazz;
				    ADAPTER_CLASSES.add(adapterClass);
				    break;
				}
			    }
			} catch (ClassNotFoundException e) {
			}
		    }
		}
	    }
	} catch (IOException e) {
	    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private static Collection<Class<?>> getInterfaces(Class<?> clazz) {
	Collection<Class<?>> accumulator = new HashSet<Class<?>>();
	getInterfaces(clazz, accumulator);
	return accumulator;
    }

    private static void getInterfaces(Class<?> clazz, Collection<Class<?>> accumulator) {
	if (clazz != null && !accumulator.contains(clazz)) {
	    for (Class<?> iface : clazz.getInterfaces()) {
		accumulator.add(iface);
		getInterfaces(iface.getSuperclass());
	    }
	    getInterfaces(clazz.getSuperclass(), accumulator);
	}
    }

    /**
     * Add your own IAdapters subclasses.
     */
    puvlic static final void registerAdapter(Class<IAdapter> clazz) {
	ADAPTER_CLASSES.add(clazz);
    }

    private PropertyResourceBundle resources;
    private Map<Class, IAdapter> adapters;
    private Statistics statistics;
    private Collection<Class> notapplicable;
    private Collection<IBatch> pending;

    protected LocLogger logger;
    protected ISession session;
    protected SystemInfoType sysinfo;

    /**
     * Initializes the logger and loads message resources from plugin[_locale].properties
     */
    protected BasePlugin() throws IOException {
	logger = JOVALMsg.getLogger();
	ClassLoader cl = BasePlugin.class.getClassLoader();
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
	if (sysinfo == null) {
	    sysinfo = SysinfoFactory.createSystemInfo(session);
	}
	return sysinfo;
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

    // Internal

    protected String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
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
	    for (Class<IAdapter> adapterClass : ADAPTER_CLASSES) {
		try {
		    IAdapter adapter = adapterClass.newInstance();
		    for (Class clazz : adapter.init(session, notapplicable)) {
			adapters.put(clazz, adapter);
			statistics.add(clazz);
		    }
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
		tk.start(object);
	    }
	}

	void stop(ObjectType object) {
	    TimeKeeper tk = timekeepers.get(object.getClass());
	    if (tk != null) {
		tk.stop(object);
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
	    logger.debug("Adapter Statistics:");
	    for (Map.Entry<Class, TimeKeeper> entry : timekeepers.entrySet()) {
		TimeKeeper tk = entry.getValue();
		if (tk.size() > 0) {
		    logger.debug("  Adapter " + entry.getKey().getName());
		    entry.getValue().dumpTrace(logger);
		}
	    }
	}
    }

    static class TimeKeeper {
	private Map<ObjectType, Duration> objects;
	private Map<String, Integer> batched;
	private Duration batchDuration;

	TimeKeeper() {
	    objects = new HashMap<ObjectType, Duration>();
	}

	void start(ObjectType object) {
	    objects.put(object, new Duration(true));
	}

	void stop(ObjectType object) {
	    Duration d = objects.get(object);
	    if (d != null) {
		d.stop();
	    }
	}

	void queue(ObjectType object) {
	    if (batched == null) {
		batched = new HashMap<String, Integer>();
		batchDuration = new Duration(false);
	    }
	    String id = object.getId();
	    if (batched.containsKey(id)) {
		batched.put(id, new Integer(batched.get(id).intValue()+1));
	    } else {
		batched.put(id, new Integer(1));
	    }
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
		logger.debug("    Batch duration: " + batchDuration.duration() + "ms");
		if (batched.size() > 0) {
		    logger.debug("    Batched Objects:");
		    TreeSet<Map.Entry<String, Integer>> ordered =
			new TreeSet<Map.Entry<String, Integer>>(new EntryStringKeyComparator<Integer>());
		    ordered.addAll(batched.entrySet());
		    for (Map.Entry<String, Integer> entry : ordered) {
			String id = entry.getKey();
			int instances = entry.getValue().intValue();
			switch(instances) {
			  case 1:
			    logger.debug("      " + id);
			    break;
			  default:
			    logger.debug("      " + id + " (" + instances + " permutations)");
			    break;
			}
		    }
		}
	    }
	    if (objects.size() > 0) {
		logger.debug("    Objects:");
		Map<String, List<Duration>> grouped = new HashMap<String, List<Duration>>();
		for (Map.Entry<ObjectType, Duration> entry : objects.entrySet()) {
		    String id = entry.getKey().getId();
		    if (!grouped.containsKey(id)) {
			grouped.put(id, new ArrayList<Duration>());
		    }
		    grouped.get(id).add(entry.getValue());
		}
		TreeSet<Map.Entry<String, List<Duration>>> ordered =
			new TreeSet<Map.Entry<String, List<Duration>>>(new EntryStringKeyComparator<List<Duration>>());
		ordered.addAll(grouped.entrySet());
		for (Map.Entry<String, List<Duration>> entry : ordered) {
		    String id = entry.getKey();
		    List<Duration> permutations = entry.getValue();
		    if (permutations.size() == 1) {
			logger.debug("      " + id + ": " + permutations.get(0).duration() + "ms");
		    } else {
			int i=0;
			for (Duration instance : permutations) {
			    logger.debug("      " + id + " (permutation " + i++ + "): " + instance.duration() + "ms");
			}
		    }
		}
	    }
	}
    }

    static class EntryStringKeyComparator<T> implements Comparator<Map.Entry<String, ? super T>> {
	EntryStringKeyComparator() {}

	public int compare(Map.Entry<String, ? super T> o1, Map.Entry<String, ? super T> o2) {
	    return o1.getKey().compareTo(o2.getKey());
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
