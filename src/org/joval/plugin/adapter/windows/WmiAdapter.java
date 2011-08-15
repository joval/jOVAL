// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntityStateAnySimpleType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateRefType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.windows.WmiObject;
import oval.schemas.definitions.windows.WmiState;
import oval.schemas.definitions.windows.WmiTest;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.WmiItem;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

/**
 * Evaluates WmiTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WmiAdapter implements IAdapter {
    private IWmiProvider wmi;

    public WmiAdapter(IWmiProvider wmi) {
	this.wmi = wmi;
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return WmiObject.class;
    }

    public boolean connect() {
	if (wmi != null) {
	    return wmi.connect();
	}
	return false;
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	}
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement <? extends ItemType>>();
	items.add(JOVALSystem.factories.sc.windows.createWmiItem(getItem((WmiObject)rc.getObject())));
	return items;
    }

    // Private

    private boolean hasResult(WmiItem item) {
	List<EntityItemAnySimpleType> results = item.getResult();
	switch(results.size()) {
	  case 0:
	    return false;
	  case 1:
	    return results.get(0).getStatus() == StatusEnumeration.EXISTS;
	  default:
	    return true;
	}
    }

    private boolean match(WmiState state, WmiItem item) throws OvalException {
	Iterator<EntityItemAnySimpleType> results = item.getResult().iterator();
	EntityStateAnySimpleType stateEntity = state.getResult();
	switch (stateEntity.getOperation()) {
	  case PATTERN_MATCH: {
	    Pattern p = Pattern.compile((String)stateEntity.getValue());
	    while(results.hasNext()) {
		EntityItemAnySimpleType resultEntity = results.next();
		Matcher m = p.matcher((String)resultEntity.getValue());
		if (m.matches()) {
		    return true;
		}
	    }
	    break;
	  }
	  case EQUALS: {
	    while(results.hasNext()) {
		EntityItemAnySimpleType resultEntity = results.next();
		if (((String)resultEntity.getValue()).equals((String)stateEntity.getValue())) {
		    return true;
		}
	    }
	    break;
	  }
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", stateEntity.getOperation()));
	}
	return false;
    }

    private WmiItem getItem(WmiObject wObj) {
	String id = wObj.getId();
	WmiItem item = JOVALSystem.factories.sc.windows.createWmiItem();
	String ns = wObj.getNamespace().getValue().toString();
	EntityItemStringType namespaceType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	namespaceType.setValue(ns);
	item.setNamespace(namespaceType);
	String wql = wObj.getWql().getValue().toString();
	EntityItemStringType wqlType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	wqlType.setValue(wql);
	item.setWql(wqlType);
	try {
	    ISWbemObjectSet objSet = wmi.execQuery(ns, wql);
	    int size = objSet.getSize();
	    if (size == 0) {
		EntityItemAnySimpleType resultType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		resultType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.getResult().add(resultType);
	    } else {
		String field = getField(wql);
		for (ISWbemObject swbObj : objSet) {
		    for (ISWbemProperty prop : swbObj.getProperties()) {
			if (prop.getName().equalsIgnoreCase(field)) {
			    EntityItemAnySimpleType resultType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
			    resultType.setValue(prop.getValueAsString());
			    item.getResult().add(resultType);
			    break;
			}
		    }
		}
	    }
	    if (!hasResult(item)) {
		item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    }
	} catch (WmiException e) {
	    item.setStatus(StatusEnumeration.ERROR);
	    item.unsetResult();
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINWMI_GENERAL", id), e);
	}
	return item;
    }

    private String getField(String wql) {
	wql = wql.toUpperCase();
	StringTokenizer tok = new StringTokenizer(wql);
	while(tok.hasMoreTokens()) {
	    String token = tok.nextToken();
	    if (token.equals("SELECT")) {
		if (tok.hasMoreTokens()) {
		    return tok.nextToken();
		}
	    }
	}
	return null;
    }
}
