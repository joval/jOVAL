// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.aix;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.aix.OslevelObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.aix.OslevelItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Provides the AIX OS Level OVAL item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OslevelAdapter implements IAdapter {
    IUnixSession session;

    public OslevelAdapter(IUnixSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {OslevelObject.class};

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
	items.add(JOVALSystem.factories.sc.aix.createOslevelItem(getItem()));
	return items;
    }

    // Private

    private OslevelItem getItem() {
	OslevelItem item = JOVALSystem.factories.sc.aix.createOslevelItem();
	EntityItemVersionType maintenanceLevel = JOVALSystem.factories.sc.core.createEntityItemVersionType();
	maintenanceLevel.setValue(session.getSystemInfo().getOsVersion());
	maintenanceLevel.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
	item.setMaintenanceLevel(maintenanceLevel);
	return item;
    }
}
