// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.independent.EnvironmentvariableObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.independent.EnvironmentvariableItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IEnvironment;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EnvironmentvariableAdapter implements IAdapter {
    private IEnvironment env;

    public EnvironmentvariableAdapter(IEnvironment env) {
	this.env = env;
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return EnvironmentvariableObject.class;
    }

    public boolean connect() {
	return env != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	EnvironmentvariableObject eObj = (EnvironmentvariableObject)rc.getObject();
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
		msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
		rc.addMessage(msg);
		JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	    }
	    break;

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", eObj.getName().getOperation()));
	}
	return items;
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
