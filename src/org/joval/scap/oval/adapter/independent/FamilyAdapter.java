// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.util.ArrayList;
import java.util.Collection;

import jsaf.intf.system.ISession;

import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.independent.FamilyObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.independent.EntityItemFamilyType;
import scap.oval.systemcharacteristics.independent.FamilyItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.sysinfo.SysinfoFactory;

/**
 * Evaluates FamilyTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FamilyAdapter implements IAdapter {
    private ISession session;
    private FamilyItem fItem = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	this.session = session;
	Collection<Class> classes = new ArrayList<Class>();
	classes.add(FamilyObject.class);
	return classes;
    }

    public Collection<FamilyItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<FamilyItem> items = new ArrayList<FamilyItem>();
	if (fItem == null) {
	    fItem = Factories.sc.independent.createFamilyItem();
	    EntityItemFamilyType familyType = Factories.sc.independent.createEntityItemFamilyType();
	    familyType.setValue(SysinfoFactory.getFamily(session).value());
	    fItem.setFamily(familyType);
	}
	items.add(fItem);
	return items;
    }
}
