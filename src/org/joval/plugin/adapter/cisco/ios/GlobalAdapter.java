// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.cisco.ios;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.ios.GlobalObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.GlobalItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS Global OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GlobalAdapter implements IAdapter {
    IIosSession session;

    public GlobalAdapter(IIosSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {GlobalObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectionException, OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	items.add(JOVALSystem.factories.sc.ios.createGlobalItem(getItem()));
	return items;
    }

    // Private

    private GlobalItem getItem() throws CollectionException {
	List<String> lines = null;
	try {
	    lines = session.getTechSupport().getData("show running-config");
	} catch (NoSuchElementException e) {
	    throw new CollectionException(e);
	}

	StringBuffer sb = new StringBuffer();
	for (String line : lines) {
	    if (sb.length() > 0) {
		sb.append('\n');
	    }
	    sb.append(line);
	}

	GlobalItem item = JOVALSystem.factories.sc.ios.createGlobalItem();
	if (sb.length() > 0) {
	    EntityItemStringType globalCommand = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    globalCommand.setValue(sb.toString());
	    item.setGlobalCommand(globalCommand);
	}
	return item;
    }
}
