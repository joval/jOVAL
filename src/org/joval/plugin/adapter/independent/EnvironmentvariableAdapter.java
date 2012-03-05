// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.independent.EnvironmentvariableObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.independent.EnvironmentvariableItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.oval.CollectException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EnvironmentvariableAdapter implements IAdapter {
    private ISession session;
    private IEnvironment environment;

    public EnvironmentvariableAdapter(ISession session) {
	this.session = session;
	environment = session.getEnvironment();
    }

    // Implement IAdapter

    private static Class[] objectClasses = {EnvironmentvariableObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException, CollectException {
	return getItems(rc, environment, null);
    }

    // Internal

    Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc, IEnvironment env, String reserved)
		throws CollectException {

	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	EnvironmentvariableObject eObj = (EnvironmentvariableObject)rc.getObject();
	String name = (String)eObj.getName().getValue();

	OperationEnumeration op = eObj.getName().getOperation();
	switch(op) {
	  case EQUALS:
	    if (env.getenv(name) != null) {
		items.add(makeItem(name, env.getenv(name), reserved));
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	    for (String varName : env) {
		if (varName.equalsIgnoreCase(name)) {
		    items.add(makeItem(varName, env.getenv(varName), reserved));
		    break;
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (String varName : env) {
		if (!name.equals(varName)) {
		    items.add(makeItem(name, env.getenv(varName), reserved));
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(name);
		for (String varName : env) {
		    if (p.matcher(varName).find()) {
			items.add(makeItem(varName, env.getenv(varName), reserved));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    JAXBElement<? extends ItemType> makeItem(String name, String value, String reserved) {
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
