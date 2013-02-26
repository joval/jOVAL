// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemObjectSet;
import jsaf.intf.windows.wmi.ISWbemProperty;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.provider.windows.wmi.WmiException;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.WmiObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.WmiItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates WmiTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WmiAdapter implements IAdapter {
    private IWindowsSession session;
    private IWmiProvider wmi;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(WmiObject.class);
	} else {
	    notapplicable.add(WmiObject.class);
	}
	return classes;
    }

    public Collection<WmiItem> getItems(ObjectType obj, IRequestContext rc) {
	wmi = session.getWmiProvider();
	Collection<WmiItem> items = new ArrayList<WmiItem>();
	try {
	    items.add(getItem((WmiObject)obj));
	} catch (NoSuchElementException e) {
	    // no results
	}
	return items;
    }

    // Private

    private WmiItem getItem(WmiObject wObj) throws NoSuchElementException {
	String id = wObj.getId();
	WmiItem item = Factories.sc.windows.createWmiItem();
	String ns = wObj.getNamespace().getValue().toString();
	EntityItemStringType namespaceType = Factories.sc.core.createEntityItemStringType();
	namespaceType.setValue(ns);
	item.setNamespace(namespaceType);
	String wql = wObj.getWql().getValue().toString();
	EntityItemStringType wqlType = Factories.sc.core.createEntityItemStringType();
	wqlType.setValue(wql);
	item.setWql(wqlType);
	try {
	    ISWbemObjectSet objSet = wmi.execQuery(ns, wql);
	    int size = objSet.getSize();
	    if (size == 0) {
		throw new NoSuchElementException(ns + ":" + wql);
	    } else {
		String field = getField(wql);
		for (ISWbemObject swbObj : objSet) {
		    for (ISWbemProperty prop : swbObj.getProperties()) {
			if (prop.getName().equalsIgnoreCase(field) && prop.getValue() != null) {
			    EntityItemAnySimpleType resultType = Factories.sc.core.createEntityItemAnySimpleType();
			    resultType.setValue(prop.getValueAsString());
			    item.getResult().add(resultType);
			    break;
			}
		    }
		}
	    }
	} catch (WmiException e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	    session.getLogger().warn(JOVALMsg.ERROR_WINWMI_GENERAL, id, e.getMessage());
	    session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return item;
    }

    private String getField(String wql) {
	wql = wql.toUpperCase();
	StringTokenizer tok = new StringTokenizer(wql);
	while(tok.hasMoreTokens()) {
	    String token = tok.nextToken();
	    if (token.equals("SELECT")) {
		if (tok.hasMoreTokens()) {
		    return tok.nextToken();
		}
	    }
	}
	return null;
    }
}
