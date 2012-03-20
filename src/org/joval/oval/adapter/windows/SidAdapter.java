// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.SidObject;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.SidItem;

import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * Collects items for the sid_sid_object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SidAdapter extends UserAdapter {
    // Implement IAdapter

    @Override
    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(SidObject.class);
	}
	return classes;
    }

    @Override
    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectException, OvalException {
	directory = session.getDirectory();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	SidObject sObj = (SidObject)rc.getObject();
	OperationEnumeration op = sObj.getTrusteeName().getOperation();

	try {
	    switch(op) {
	      case EQUALS: {
		String netbiosName = directory.getQualifiedNetbiosName((String)sObj.getTrusteeName().getValue());
		items.add(makeItem(directory.queryPrincipal(netbiosName)));
		break;
	      }

	      case NOT_EQUAL: {
		String netbiosName = directory.getQualifiedNetbiosName((String)sObj.getTrusteeName().getValue());
		for (IPrincipal p : directory.queryAllPrincipals()) {
		    if (!p.getNetbiosName().equals(netbiosName)) {
			items.add(makeItem(p));
		    }
		}
		break;
	      }

	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile((String)sObj.getTrusteeName().getValue());
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (p.matcher(principal.getNetbiosName()).find()) {
			    items.add(makeItem(principal));
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		    rc.addMessage(msg);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		break;
    
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (NoSuchElementException e) {
	    // No match.
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private JAXBElement<? extends ItemType> makeItem(IPrincipal principal) {
	SidItem item = Factories.sc.windows.createSidItem();
	EntityItemStringType trusteeSidType = Factories.sc.core.createEntityItemStringType();
	trusteeSidType.setValue(principal.getSid());
	trusteeSidType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeSid(trusteeSidType);

	boolean builtin = false;
	switch(principal.getType()) {
	  case USER:
	    if (directory.isBuiltinUser(principal.getNetbiosName())) {
		builtin = true;
 	    }
	    break;
	  case GROUP:
	    if (directory.isBuiltinGroup(principal.getNetbiosName())) {
		builtin = true;
 	    }
	    break;
	}
	EntityItemStringType trusteeNameType = Factories.sc.core.createEntityItemStringType();
	if (builtin) {
	    trusteeNameType.setValue(principal.getName());
	} else {
	    trusteeNameType.setValue(principal.getNetbiosName());
	}
	trusteeNameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeName(trusteeNameType);

	EntityItemStringType trusteeDomainType = Factories.sc.core.createEntityItemStringType();
	trusteeDomainType.setValue(principal.getDomain());
	trusteeDomainType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeDomain(trusteeDomainType);

	return Factories.sc.windows.createSidItem(item);
    }
}
