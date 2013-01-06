// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.aix.OslevelObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.aix.OslevelItem;
import oval.schemas.results.core.ResultEnumeration;

import jsaf.intf.system.IBaseSession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

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

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(OslevelObject.class);
	}
	return classes;
    }

    public Collection<OslevelItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<OslevelItem> items = new Vector<OslevelItem>();
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
