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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IEnvironment;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EnvironmentvariableAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IEnvironment env;

    public EnvironmentvariableAdapter(IEnvironment env) {
	this.env = env;
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
	EnvironmentvariableObject eObj = (EnvironmentvariableObject)obj;
	String name = (String)eObj.getName().getValue();

	switch(eObj.getName().getOperation()) {
	  case EQUALS:
	    if (env.getenv(name) != null) {
		items.add(makeItem(name, env.getenv(name)));
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	    for (String varName : env) {
		if (varName.equalsIgnoreCase(name)) {
		    items.add(makeItem(varName, env.getenv(varName)));
		    break;
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (String varName : env) {
		if (!name.equals(varName)) {
		    items.add(makeItem(name, env.getenv(varName)));
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(name);
		for (String varName : env) {
		    if (p.matcher(varName).find()) {
			items.add(makeItem(varName, env.getenv(varName)));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage("STATUS_NOT_FOUND", name, eObj.getId()));
		ctx.addObjectMessage(obj.getId(), msg);
		ctx.log(Level.WARNING, e.getMessage(), e);
	    }
	    break;

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", eObj.getName().getOperation()));
	}

	if (items.size() == 0) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALSystem.getMessage("STATUS_NOT_FOUND", name, eObj.getId()));
	    ctx.addObjectMessage(obj.getId(), msg);
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	EnvironmentvariableState state = (EnvironmentvariableState)st;
	EnvironmentvariableItem item = (EnvironmentvariableItem)it;

	if (state.isSetName()) {
	    return ctx.test(state.getName(), item.getName());
	} else if (state.isSetValue()) {
	    return ctx.test(state.getValue(), item.getValue());
	}
	throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_EMPTY", state.getId()));
    }

    // Internal

    private JAXBElement<EnvironmentvariableItem> makeItem(String name, String value) {
	EnvironmentvariableItem item = JOVALSystem.factories.sc.independent.createEnvironmentvariableItem();
	EntityItemStringType nameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	nameType.setValue(name);
	item.setName(nameType);
	EntityItemAnySimpleType valueType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
	valueType.setValue(value);
	item.setValue(valueType);
	return JOVALSystem.factories.sc.independent.createEnvironmentvariableItem(item);
    }
}
