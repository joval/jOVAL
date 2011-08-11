// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.FamilyObject;
import oval.schemas.definitions.independent.FamilyState;
import oval.schemas.definitions.independent.FamilyTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.independent.EntityItemFamilyType;
import oval.schemas.systemcharacteristics.independent.FamilyItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.plugin.IPlugin;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates FamilyTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FamilyAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IPlugin plugin;

    public FamilyAdapter(IPlugin plugin) {
	this.plugin = plugin;
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return FamilyObject.class;
    }

    public Class getStateClass() {
	return FamilyState.class;
    }

    public Class getItemClass() {
	return FamilyItem.class;
    }

    public boolean connect() {
	return plugin != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	items.add(JOVALSystem.factories.sc.independent.createFamilyItem(getItem()));
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	FamilyState state = (FamilyState)st;
	FamilyItem item = (FamilyItem)it;

	return ctx.test(state.getFamily(), item.getFamily());
    }

    // Private

    FamilyItem fItem = null;

    private FamilyItem getItem() {
	if (fItem == null) {
	    fItem = JOVALSystem.factories.sc.independent.createFamilyItem();
	    EntityItemFamilyType familyType = JOVALSystem.factories.sc.independent.createEntityItemFamilyType();
	    familyType.setValue(plugin.getFamily().value());
	    fItem.setFamily(familyType);
	}
	return fItem;
    }
}
