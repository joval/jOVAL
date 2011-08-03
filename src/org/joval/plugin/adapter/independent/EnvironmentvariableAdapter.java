// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.EnvironmentvariableObject;
import oval.schemas.definitions.independent.EnvironmentvariableState;
import oval.schemas.definitions.independent.EnvironmentvariableTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.independent.EnvironmentvariableItem;
import oval.schemas.systemcharacteristics.independent.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IEnvironment;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.TypeTools;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EnvironmentvariableAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IEnvironment env;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory independentFactory;

    public EnvironmentvariableAdapter(IEnvironment env) {
	this.env = env;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	independentFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return EnvironmentvariableObject.class;
    }

    public Class getStateClass() {
	return EnvironmentvariableState.class;
    }

    public Class getItemClass() {
	return EnvironmentvariableItem.class;
    }

    public boolean connect() {
	return env != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    EnvironmentvariableObject eObj = (EnvironmentvariableObject)obj;
	    items.add(getItem(eObj));
	} catch (NoSuchElementException e) {
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    ctx.addObjectMessage(obj.getId(), msg);
	    ctx.log(Level.WARNING, e.getMessage(), e);
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	EnvironmentvariableState state = (EnvironmentvariableState)st;
	EnvironmentvariableItem item = (EnvironmentvariableItem)it;

	if (state.isSetName()) {
	    if (TypeTools.compare(state.getName(), item.getName())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	} else if (state.isSetValue()) {
	    if (TypeTools.compare(state.getValue(), item.getValue())) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_EMPTY", state.getId()));
	}
    }

    // Internal

    private JAXBElement<EnvironmentvariableItem> getItem(EnvironmentvariableObject variable) throws NoSuchElementException {
	String varName = (String)variable.getName().getValue();
	String varValue = env.getenv(varName);
	if (varValue == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("STATUS_NOT_FOUND", varName, variable.getId()));
	}
	EnvironmentvariableItem item = independentFactory.createEnvironmentvariableItem();

	EntityItemStringType name = coreFactory.createEntityItemStringType();
	name.setValue(varName);
	item.setName(name);
	EntityItemAnySimpleType value = coreFactory.createEntityItemAnySimpleType();
	value.setValue(varValue);
	item.setValue(value);

	return independentFactory.createEnvironmentvariableItem(item);
    }
}
