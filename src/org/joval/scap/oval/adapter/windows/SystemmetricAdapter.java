// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.EntityObjectSystemMetricIndexType;
import scap.oval.definitions.windows.SystemmetricObject;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemSystemMetricIndexType;
import scap.oval.systemcharacteristics.windows.SystemmetricItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:systemmetric_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SystemmetricAdapter implements IAdapter {
    private IWindowsSession session;
    private Map<String, SystemmetricItem> metrics = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(SystemmetricObject.class);
	} else {
	    notapplicable.add(SystemmetricObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw error;
	} else if (metrics == null) {
	    init();
	}
	Collection<SystemmetricItem> items = new ArrayList<SystemmetricItem>();
	SystemmetricObject sObj = (SystemmetricObject)obj;
	EntityObjectSystemMetricIndexType index = sObj.getIndex();
	OperationEnumeration op = index.getOperation();
	String s = (String)index.getValue();
	switch(op) {
	  case EQUALS:
	    if (metrics.containsKey(s)) {
		items.add(metrics.get(s));
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	    for (Map.Entry<String, SystemmetricItem> entry : metrics.entrySet()) {
		if (s.equalsIgnoreCase(entry.getKey())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (Map.Entry<String, SystemmetricItem> entry : metrics.entrySet()) {
		if (!s.equals(entry.getKey())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    // Private

    private void init() throws CollectException {
	try {
	    //
	    // Get a runspace if there are any in the pool, or create a new one, and load the Get-SystemMetrics
	    // Powershell module code.
	    //
	    IRunspace runspace = null;
	    IWindowsSession.View view = session.getNativeView();
	    for (IRunspace rs : session.getRunspacePool().enumerate()) {
		if (rs.getView() == view) {
		    runspace = rs;
		    break;
		}
	    }
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn(view);
	    }
	    runspace.loadModule(getClass().getResourceAsStream("Systemmetric.psm1"));

	    //
	    // Run the Get-SystemMetrics module and parse the output
	    //
	    metrics = new HashMap<String, SystemmetricItem>();
	    for (String line : runspace.invoke("Get-SystemMetrics").split("\n")) {
		int ptr = line.indexOf(":");
		String key=null, val=null;
		if (ptr > 0) {
		    key = line.substring(0,ptr).trim();
		    val = line.substring(ptr+1).trim();
		    try {
			EntityItemSystemMetricIndexType index = Factories.sc.windows.createEntityItemSystemMetricIndexType();
			index.setDatatype(SimpleDatatypeEnumeration.STRING.value());
			index.setValue(key);

			EntityItemIntType value = Factories.sc.core.createEntityItemIntType();
			value.setDatatype(SimpleDatatypeEnumeration.INT.value());
			value.setValue(new Integer(val).toString());

			SystemmetricItem item = Factories.sc.windows.createSystemmetricItem();
			item.setIndex(index);
			item.setValue(value);
			metrics.put(key, item);
		    } catch (IllegalArgumentException e) {
			session.getLogger().warn(JOVALMsg.ERROR_WIN_SYSTEMMETRIC_VALUE, e.getMessage(), key);
		    }
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
