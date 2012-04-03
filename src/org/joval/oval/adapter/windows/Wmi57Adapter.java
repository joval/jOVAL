// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;

import oval.schemas.common.ComplexDatatypeEnumeration;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.Wmi57Object;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.Wmi57Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
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

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(Wmi57Object.class);
	}
	return classes;
    }

    public Collection<Wmi57Item> getItems(ObjectType obj, IRequestContext rc) throws OvalException {
	//
	// Grab a fresh WMI provider in case there has been a reconnect since init.
	//
	wmi = session.getWmiProvider();
	Wmi57Object wObj = (Wmi57Object)obj;
	String id = wObj.getId();

	String ns = (String)wObj.getNamespace().getValue();
	String wql = (String)wObj.getWql().getValue();

	Collection<Wmi57Item> items = new Vector<Wmi57Item>();
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
	    if (size == 0) {
		EntityItemRecordType record = Factories.sc.core.createEntityItemRecordType();
		record.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.getResult().add(record);
	    } else {
		for (ISWbemObject swbObj : objSet) {
		    EntityItemRecordType record = Factories.sc.core.createEntityItemRecordType();
		    record.setDatatype(ComplexDatatypeEnumeration.RECORD.value());
		    for (ISWbemProperty prop : swbObj.getProperties()) {
			EntityItemFieldType field = Factories.sc.core.createEntityItemFieldType();
			field.setName(prop.getName().toLowerCase()); // upper-case chars are not allowed per the OVAL spec.
			field.setValue(prop.getValueAsString());
			record.getField().add(field);
		    }
		    item.getResult().add(record);
		}
	    }
	} catch (Exception e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    item.unsetResult();
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);

	    if (e instanceof WmiException) {
		session.getLogger().debug(JOVALMsg.ERROR_WINWMI_GENERAL, id);
		session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } else {
		session.getLogger().warn(JOVALMsg.ERROR_WINWMI_GENERAL, id);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	items.add(item);
	return items;
    }
}
