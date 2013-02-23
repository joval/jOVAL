// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.aix.OslevelObject;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemVersionType;
import scap.oval.systemcharacteristics.aix.OslevelItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Provides the AIX OS Level OVAL item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OslevelAdapter implements IAdapter {
    IUnixSession session;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.AIX) {
	    this.session = (IUnixSession)session;
	    classes.add(OslevelObject.class);
	} else {
	    notapplicable.add(OslevelObject.class);
	}
	return classes;
    }

    public Collection<OslevelItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<OslevelItem> items = new ArrayList<OslevelItem>();
	try {
	    OslevelItem item = Factories.sc.aix.createOslevelItem();
	    EntityItemVersionType maintenanceLevel = Factories.sc.core.createEntityItemVersionType();
	    maintenanceLevel.setValue(SafeCLI.exec("uname -r", session, IUnixSession.Timeout.S));
	    maintenanceLevel.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	    item.setMaintenanceLevel(maintenanceLevel);
	    items.add(item);
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	return items;
    }
}
