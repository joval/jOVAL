// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.StringTokenizer;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemObjectSet;
import jsaf.intf.windows.wmi.ISWbemProperty;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.provider.windows.wmi.WmiException;

import scap.oval.common.ComplexDatatypeEnumeration;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.Wmi57Object;
import scap.oval.systemcharacteristics.core.EntityItemFieldType;
import scap.oval.systemcharacteristics.core.EntityItemRecordType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.Wmi57Item;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates WmiTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Wmi57Adapter implements IAdapter {
    private IWindowsSession session;
    private IWmiProvider wmi;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(Wmi57Object.class);
	} else {
	    notapplicable.add(Wmi57Object.class);
	}
	return classes;
    }

    public Collection<Wmi57Item> getItems(ObjectType obj, IRequestContext rc) {
	//
	// Grab a fresh WMI provider in case there has been a reconnect since init.
	//
	wmi = session.getWmiProvider();
	Wmi57Object wObj = (Wmi57Object)obj;
	String id = wObj.getId();

	String ns = (String)wObj.getNamespace().getValue();
	String wql = (String)wObj.getWql().getValue();

	Collection<Wmi57Item> items = new ArrayList<Wmi57Item>();
	Wmi57Item item = Factories.sc.windows.createWmi57Item();
	try {
	    EntityItemStringType namespaceType = Factories.sc.core.createEntityItemStringType();
	    namespaceType.setValue(ns);
	    item.setNamespace(namespaceType);

	    EntityItemStringType wqlType = Factories.sc.core.createEntityItemStringType();
	    wqlType.setValue(wql);
	    item.setWql(wqlType);

	    ISWbemObjectSet objSet = wmi.execQuery(ns, wql);
	    int size = objSet.getSize();
	    for (ISWbemObject swbObj : objSet) {
		EntityItemRecordType record = Factories.sc.core.createEntityItemRecordType();
		record.setDatatype(ComplexDatatypeEnumeration.RECORD.value());
		for (ISWbemProperty prop : swbObj.getProperties()) {
		    if (prop.getValue() != null) {
			EntityItemFieldType field = Factories.sc.core.createEntityItemFieldType();
			field.setName(prop.getName().toLowerCase()); // upper-case chars are not allowed per the OVAL spec.
			field.setValue(prop.getValueAsString());
			record.getField().add(field);
		    }
		}
		item.getResult().add(record);
	    }
	} catch (WmiException e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    item.unsetResult();
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);

	    session.getLogger().warn(JOVALMsg.ERROR_WINWMI_GENERAL, id, e.getMessage());
	    session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	if (item.isSetResult()) {
	    items.add(item);
	}
	return items;
    }
}
