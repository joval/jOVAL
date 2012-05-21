// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.cisco.ios;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.ios.LineObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.LineItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.PropertyUtil;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Provides Cisco IOS Line OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LineAdapter implements IAdapter {
    private IIosSession session;
    private long readTimeout = 0;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    readTimeout = session.getProperties().getLongProperty(IIosSession.PROP_READ_TIMEOUT);
	    this.session = (IIosSession)session;
	    classes.add(LineObject.class);
	}
	return classes;
    }

    public Collection<LineItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Collection<LineItem> items = new Vector<LineItem>();

	LineObject lObj = (LineObject)obj;
	if (lObj.getShowSubcommand() == null || !lObj.getShowSubcommand().isSetValue()) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_NO_SHOW), FlagEnumeration.ERROR);
	}
	String subcommand = (String)lObj.getShowSubcommand().getValue();
	OperationEnumeration op = lObj.getShowSubcommand().getOperation();
	switch(op) {
	  case EQUALS:
            try {
		items.add(getItem(subcommand));
	    } catch (IllegalStateException e) {
		throw new CollectException(e, FlagEnumeration.NOT_COLLECTED);
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_IOS_SHOW, subcommand, e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
		session.getLogger().warn(s);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	return items;
    }

    // Private

    private LineItem getItem(String subcommand) throws Exception {
	if (!subcommand.toLowerCase().startsWith("show ")) {
	    subcommand = new StringBuffer("show ").append(subcommand).toString();
	}

	String stdout = null;
	try {
	    stdout = session.getTechSupport().getData(subcommand);
	} catch (NoSuchElementException e) {
	    SafeCLI.ExecData data = SafeCLI.execData(subcommand, null, session, readTimeout);
	    byte[] raw = data.getData();
	    if (raw != null) {
		stdout = new String(raw, StringTools.UTF8);
	    }
	}

	LineItem item = Factories.sc.ios.createLineItem();
	EntityItemStringType showSubcommand = Factories.sc.core.createEntityItemStringType();
	showSubcommand.setValue(subcommand);
	item.setShowSubcommand(showSubcommand);
	if (stdout != null) {
	    EntityItemStringType configLine = Factories.sc.core.createEntityItemStringType();
	    configLine.setValue(stdout);
	    item.setConfigLine(configLine);
	}
	return item;
    }
}
