// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.definitions.windows.Wmi57Object;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.Wmi57Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates WmiTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Wmi57Adapter implements IAdapter {
    private IWindowsSession session;
    private IWmiProvider wmi;

    public Wmi57Adapter(IWindowsSession session) {
	this.session = session;
	wmi = session.getWmiProvider();
    }

    // Implement IAdapter

    private static Class[] objectClasses = {Wmi57Object.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	if (wmi != null) {
	    return wmi.connect();
	}
	return false;
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	}
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectionException, OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement <? extends ItemType>>();
	items.add(JOVALSystem.factories.sc.windows.createWmi57Item(getItem((Wmi57Object)rc.getObject())));
	return items;
    }

    // Private

    private Wmi57Item getItem(Wmi57Object wObj) {
	String id = wObj.getId();
	Wmi57Item item = JOVALSystem.factories.sc.windows.createWmi57Item();
	String ns = wObj.getNamespace().getValue().toString();
	EntityItemStringType namespaceType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	namespaceType.setValue(ns);
	item.setNamespace(namespaceType);
	String wql = wObj.getWql().getValue().toString();
	EntityItemStringType wqlType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	wqlType.setValue(wql);
	item.setWql(wqlType);
	try {
	    ISWbemObjectSet objSet = wmi.execQuery(ns, wql);
	    int size = objSet.getSize();
	    if (size == 0) {
		EntityItemRecordType record = JOVALSystem.factories.sc.core.createEntityItemRecordType();
		record.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.getResult().add(record);
	    } else {
		for (ISWbemObject swbObj : objSet) {
		    EntityItemRecordType record = JOVALSystem.factories.sc.core.createEntityItemRecordType();
		    for (ISWbemProperty prop : swbObj.getProperties()) {
			EntityItemFieldType field = JOVALSystem.factories.sc.core.createEntityItemFieldType();
			field.setName(prop.getName());
			field.setValue(prop.getValueAsString());
			record.getField().add(field);
		    }
		    item.getResult().add(record);
		}
	    }
	} catch (Exception e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    item.unsetResult();
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);

	    if (e instanceof WmiException) {
		session.getLogger().debug(JOVALMsg.ERROR_WINWMI_GENERAL, id);
		session.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } else {
		session.getLogger().warn(JOVALMsg.ERROR_WINWMI_GENERAL, id);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return item;
    }
}
