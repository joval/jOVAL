// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.independent;

import java.util.Collection;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.definitions.independent.FamilyObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.independent.EntityItemFamilyType;
import oval.schemas.systemcharacteristics.independent.FamilyItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.oval.sysinfo.SysinfoFactory;

/**
 * Evaluates FamilyTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FamilyAdapter implements IAdapter {
    private IBaseSession session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	this.session = session;
	Collection<Class> classes = new Vector<Class>();
	classes.add(FamilyObject.class);
	return classes;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	items.add(Factories.sc.independent.createFamilyItem(getItem()));
	return items;
    }

    // Private

    FamilyItem fItem = null;

    private FamilyItem getItem() {
	if (fItem == null) {
	    fItem = Factories.sc.independent.createFamilyItem();
	    EntityItemFamilyType familyType = Factories.sc.independent.createEntityItemFamilyType();
	    familyType.setValue(SysinfoFactory.getFamily(session).value());
	    fItem.setFamily(familyType);
	}
	return fItem;
    }
}
