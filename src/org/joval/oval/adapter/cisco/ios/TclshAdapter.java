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
import oval.schemas.definitions.ios.TclshObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.ios.TclshItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS Tclsh OVAL item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TclshAdapter implements IAdapter {
    private static final String ERROR = "Line has invalid autocommand \"tclsh\"";

    private IIosSession session;
    private long readTimeout;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    readTimeout = session.getProperties().getLongProperty(IIosSession.PROP_READ_TIMEOUT);
	    classes.add(TclshObject.class);
	}
	return classes;
    }

    public Collection<TclshItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<TclshItem> items = new Vector<TclshItem>();
	try {
	    boolean result = true;
	    for (String line : SafeCLI.multiLine("tclsh", session, readTimeout)) {
		if (ERROR.equalsIgnoreCase(line)) {
		    result = false;
		}
	    }
	    TclshItem item = Factories.sc.ios.createTclshItem();
	    EntityItemBoolType available = Factories.sc.core.createEntityItemBoolType();
	    if (result) {
		available.setValue("true");
	    } else {
		available.setValue("false");
	    }
	    available.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setAvailable(available);
	    items.add(item);
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }
}
