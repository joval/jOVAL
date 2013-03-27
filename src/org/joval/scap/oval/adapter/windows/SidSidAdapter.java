// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IGroup;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.wmi.WmiException;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.SidSidObject;
import scap.oval.definitions.windows.SidSidBehaviors;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.SidSidItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for the sid_sid_object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SidSidAdapter implements IAdapter {
    private IWindowsSession session;
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(SidSidObject.class);
	} else {
	    notapplicable.add(SidSidObject.class);
	}
	return classes;
    }

    public Collection<SidSidItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	Collection<SidSidItem> items = new ArrayList<SidSidItem>();
	SidSidObject sObj = (SidSidObject)obj;
	OperationEnumeration op = sObj.getTrusteeSid().getOperation();
	String sid = (String)sObj.getTrusteeSid().getValue();
	SidSidBehaviors behaviors = sObj.getBehaviors();

	try {
	    switch(op) {
	      case EQUALS:
		items.addAll(makeItems(directory.queryPrincipalBySid(sid), behaviors));
		break;

	      case NOT_EQUAL:
		for (IPrincipal p : directory.queryAllPrincipals()) {
		    if (!p.getSid().equals(sid)) {
			items.addAll(makeItems(p, behaviors));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(sid);
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (p.matcher(principal.getSid()).find()) {
			    items.addAll(makeItems(principal, behaviors));
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
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private List<SidSidItem> makeItems(IPrincipal principal, SidSidBehaviors behaviors)
		throws WmiException {

	List<SidSidItem> items = new ArrayList<SidSidItem>();
	boolean includeGroups = true;
	boolean resolveGroups = false;
	if (behaviors != null) {
	    includeGroups = behaviors.getIncludeGroup();
	    resolveGroups = behaviors.getResolveGroup();
	}
	for (IPrincipal p : directory.getAllPrincipals(principal, includeGroups, resolveGroups)) {
	    items.add(makeItem(p));
	}
	return items;
    }

    private SidSidItem makeItem(IPrincipal principal) {
	SidSidItem item = Factories.sc.windows.createSidSidItem();
	EntityItemStringType trusteeSidType = Factories.sc.core.createEntityItemStringType();
	trusteeSidType.setValue(principal.getSid());
	trusteeSidType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setTrusteeSid(trusteeSidType);

	EntityItemStringType trusteeNameType = Factories.sc.core.createEntityItemStringType();
	if (principal.isBuiltin()) {
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
	return item;
    }
}
