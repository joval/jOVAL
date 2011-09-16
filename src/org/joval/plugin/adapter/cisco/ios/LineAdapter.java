// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.cisco.ios;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.ios.LineObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.LineItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.IBaseSession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Provides Cisco IOS Line OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LineAdapter implements IAdapter {
    IBaseSession session;

    public LineAdapter(IBaseSession session) {
	this.session = session;
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return LineObject.class;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	LineObject lObj = (LineObject)rc.getObject();
	String subcommand = (String)lObj.getShowSubcommand().getValue();
	OperationEnumeration op = lObj.getShowSubcommand().getOperation();
	switch(op) {
	  case EQUALS:
            try {
		items.add(JOVALSystem.factories.sc.ios.createLineItem(getItem((subcommand))));
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALSystem.getMessage(JOVALMsg.ERROR_IOS_SHOW, subcommand, e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
		JOVALSystem.getLogger().warn(s);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
	}

	return items;
    }

    // Private

    private LineItem getItem(String subcommand) throws Exception {
	StringBuffer sb = new StringBuffer();
	IProcess p = session.createProcess("show " + subcommand);
	p.start();
	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	try {
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		if (sb.length() > 0) {
		    sb.append('\n');
		}
		sb.append(line);
	    }
	} finally {
	    reader.close();
	}

	LineItem item = JOVALSystem.factories.sc.ios.createLineItem();
	EntityItemStringType showSubcommand = JOVALSystem.factories.sc.core.createEntityItemStringType();
	showSubcommand.setValue(subcommand);
	item.setShowSubcommand(showSubcommand);
	if (sb.length() > 0) {
	    EntityItemStringType configLine = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    configLine.setValue(sb.toString());
	    item.setConfigLine(configLine);
	}
	return item;
    }
}
