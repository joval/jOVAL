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

import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.VariableObject;
import oval.schemas.definitions.independent.VariableState;
import oval.schemas.definitions.independent.VariableTest;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.independent.EntityItemVariableRefType;
import oval.schemas.systemcharacteristics.independent.VariableItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.plugin.IPlugin;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.oval.util.CheckData;
import org.joval.util.JOVALSystem;

/**
 * Evaluates VariableTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VariableAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IPlugin plugin;

    public VariableAdapter() {
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return VariableObject.class;
    }

    public Class getStateClass() {
	return VariableState.class;
    }

    public Class getItemClass() {
	return VariableItem.class;
    }

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	VariableObject vObj = (VariableObject)obj;
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	List<String> values = ctx.resolve((String)vObj.getVarRef().getValue(), vars);
	if (values.size() > 0) {
	    VariableItem item = JOVALSystem.factories.sc.independent.createVariableItem();
	    EntityItemVariableRefType ref = JOVALSystem.factories.sc.independent.createEntityItemVariableRefType();
	    ref.setValue(vObj.getVarRef().getValue());
	    for (String value : values) {
		EntityItemAnySimpleType valueType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		valueType.setValue(value);
		item.getValue().add(valueType);
	    }
	    items.add(JOVALSystem.factories.sc.independent.createVariableItem(item));
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	VariableState state = (VariableState)st;
	VariableItem item = (VariableItem)it;

	if (state.isSetVarRef()) {
	    ResultEnumeration result = ctx.test(state.getVarRef(), item.getVarRef());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetValue()) {
	    if (item.isSetValue()) {
		CheckData cd = new CheckData();
		for (EntityItemAnySimpleType itemType : item.getValue()) {
		    cd.addResult(ctx.test(state.getValue(), itemType));
		}
		ResultEnumeration result = cd.getResult(state.getValue().getEntityCheck());
		if (result != ResultEnumeration.TRUE) {
		    return result;
		}
	    } else {
		return ResultEnumeration.FALSE;
	    }
	}
	return ResultEnumeration.TRUE;
    }
}
