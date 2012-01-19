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
import org.joval.io.PerishableReader;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Provides Cisco IOS Tclsh OVAL item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TclshAdapter implements IAdapter {
    IIosSession session;
    long readTimeout;

    public TclshAdapter(IIosSession session) {
	this.session = session;
	readTimeout = JOVALSystem.getLongProperty(JOVALSystem.PROP_IOS_READ_TIMEOUT);
    }

    // Implement IAdapter

    private static Class[] objectClasses = {TclshObject.class};

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
	items.add(JOVALSystem.factories.sc.ios.createTclshItem(getItem()));
	return items;
    }

    // Private

    private static final String ERROR = "Line has invalid autocommand \"tclsh\"";

    private TclshItem getItem() throws CollectionException {
	try {
	    boolean result = true;
	    for (String line : SafeCLI.multiLine("tclsh", session, readTimeout)) {
		if (ERROR.equalsIgnoreCase(line)) {
		    result = false;
		}
	    }

	    TclshItem item = JOVALSystem.factories.sc.ios.createTclshItem();
	    EntityItemBoolType available = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    if (result) {
		available.setValue("true");
	    } else {
		available.setValue("false");
	    }
	    available.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setAvailable(available);
	    return item;
	} catch (Exception e) {
	    throw new CollectionException(e);
	}
    }
}
