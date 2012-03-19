// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.cisco.ios;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.ios.InterfaceObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.InterfaceItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS SNMP OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class InterfaceAdapter implements IAdapter {
    IIosSession session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    classes.add(InterfaceObject.class);
	}
	return classes;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    for (InterfaceItem item : getItems()) {
		items.add(JOVALSystem.factories.sc.ios.createInterfaceItem(item));
	    }
	} catch (NoSuchElementException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_TECH_SHOW, ITechSupport.GLOBAL));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    private Collection<InterfaceItem> getItems() throws NoSuchElementException {
	List<String> lines = session.getTechSupport().getData(ITechSupport.GLOBAL);
	InterfaceItem item = null;
	Collection<InterfaceItem> items = new Vector<InterfaceItem>();
	for (String line : lines) {
	    if (line.toLowerCase().startsWith("interface ")) {
		if (item != null) {
		    items.add(item);
		}
		item = JOVALSystem.factories.sc.ios.createInterfaceItem();

		EntityItemStringType name = JOVALSystem.factories.sc.core.createEntityItemStringType();
		name.setValue(line.substring(10).trim());
		item.setName(name);
	    } else if (item != null) {
		if (line.startsWith(" ")) {
		    String command = line.substring(1);
		    if (command.startsWith("!")) {
			// skip comment
		    } else if (line.startsWith("ip directed-broadcast")) {
			EntityItemStringType idbc = JOVALSystem.factories.sc.core.createEntityItemStringType();
			idbc.setValue(command);
			item.setIpDirectedBroadcastCommand(idbc);
		    } else if (line.startsWith("no ip directed-broadcast")) {
			EntityItemStringType nidbc = JOVALSystem.factories.sc.core.createEntityItemStringType();
			nidbc.setValue(command);
			item.setNoIpDirectedBroadcastCommand(nidbc);
		    } else if (line.indexOf("proxy-arp") != -1) {
			EntityItemStringType pac = JOVALSystem.factories.sc.core.createEntityItemStringType();
			pac.setValue(command);
			item.setProxyArpCommand(pac);
		    } else if (line.indexOf("shutdown") != -1) {
			EntityItemStringType sc = JOVALSystem.factories.sc.core.createEntityItemStringType();
			sc.setValue(command);
			item.setShutdownCommand(sc);
		    }
		} else {
		    items.add(item);
		    item = null;
		}
	    }
	}
	if (item != null) {
	    items.add(item);
	}
	return items;
    }
}
