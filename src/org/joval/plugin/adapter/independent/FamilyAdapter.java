// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.FamilyObject;
import oval.schemas.definitions.independent.FamilyState;
import oval.schemas.definitions.independent.FamilyTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.independent.EntityItemFamilyType;
import oval.schemas.systemcharacteristics.independent.FamilyItem;
import oval.schemas.systemcharacteristics.independent.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates FamilyTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FamilyAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IDefinitions definitions;
    private IPlugin plugin;
    private ObjectFactory independentFactory;

    public FamilyAdapter(IPlugin plugin) {
	this.plugin = plugin;
	independentFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
	definitions = ctx.getDefinitions();
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	Iterator<ObjectType> iter = definitions.iterateLeafObjects(FamilyObject.class);
	for (int i=0; iter.hasNext(); i++) {
	    if (i > 0) {
		throw new OvalException(JOVALSystem.getMessage("ERROR_FAMILY_OVERFLOW"));
	    }
	    FamilyObject fObj = (FamilyObject)iter.next();
	    ctx.status(fObj.getId());
	    FamilyItem item = getItem();
	    sc.setObject(fObj.getId(), fObj.getComment(), fObj.getVersion(), FlagEnumeration.COMPLETE, null);
	    BigInteger itemId = sc.storeItem(independentFactory.createFamilyItem(item));
	    sc.relateItem(fObj.getId(), itemId);
	}
    }

    public List<? extends ItemType> getItems(ObjectType ot) throws OvalException {
	Vector<FamilyItem> v = new Vector<FamilyItem>();
	v.add(getItem());
	return v;
    }

    public Class getObjectClass() {
	return FamilyObject.class;
    }

    public Class getTestClass() {
	return FamilyTest.class;
    }

    public Class getStateClass() {
	return FamilyState.class;
    }

    public Class getItemClass() {
	return FamilyItem.class;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	FamilyState state = (FamilyState)st;
	FamilyItem item = (FamilyItem)it;

	switch (state.getFamily().getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    if (((String)item.getFamily().getValue()).equalsIgnoreCase((String)state.getFamily().getValue())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case EQUALS:
	    if (((String)item.getFamily().getValue()).equals((String)state.getFamily().getValue())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getFamily().getOperation()));
	}
    }

    // Private

    private FamilyItem getItem() {
	FamilyItem item = independentFactory.createFamilyItem();
	EntityItemFamilyType familyType = independentFactory.createEntityItemFamilyType();
	familyType.setValue(plugin.getFamily().value());
	item.setFamily(familyType);
	return item;
    }
}
