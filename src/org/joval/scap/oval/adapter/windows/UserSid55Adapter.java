// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IGroup;
import jsaf.intf.windows.identity.IUser;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.wmi.WmiException;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.UserSid55Object;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.UserSidItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for the user_sid55_object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserSid55Adapter implements IAdapter {
    protected IWindowsSession session;
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(UserSid55Object.class);
	} else {
	    notapplicable.add(UserSid55Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	Collection<UserSidItem> items = new ArrayList<UserSidItem>();
	UserSid55Object uObj = (UserSid55Object)obj;
	String sid = (String)uObj.getUserSid().getValue();
	OperationEnumeration op = uObj.getUserSid().getOperation();

	try {
	    switch(op) {
	      case EQUALS:
		items.add(makeItem(directory.queryUserBySid(sid)));
		break;
    
	      case NOT_EQUAL:
		for (IUser u : directory.queryAllUsers()) {
		    if (!u.getSid().equals(sid)) {
			items.add(makeItem(u));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(sid);
		    for (IUser u : directory.queryAllUsers()) {
			if (p.matcher(u.getSid()).find()) {
			    items.add(makeItem(u));
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

    private UserSidItem makeItem(IUser user) {
	UserSidItem item = Factories.sc.windows.createUserSidItem();
	EntityItemStringType userSidType = Factories.sc.core.createEntityItemStringType();
	userSidType.setValue(user.getSid());
	item.setUserSid(userSidType);
	EntityItemBoolType enabledType = Factories.sc.core.createEntityItemBoolType();
	enabledType.setValue(user.isEnabled() ? "true" : "false");
	enabledType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setEnabled(enabledType);
	Collection<String> groupNetbiosNames = user.getGroupNetbiosNames();
	if (groupNetbiosNames != null) {
	    for (String groupNetbiosName : groupNetbiosNames) {
		try {
		    IGroup group = directory.queryGroup(groupNetbiosName);
		    EntityItemStringType groupSidType = Factories.sc.core.createEntityItemStringType();
		    groupSidType.setValue(group.getSid());
		    item.getGroupSid().add(groupSidType);
		} catch (IllegalArgumentException e) {
		} catch (NoSuchElementException e) {
		} catch (WmiException e) {
		}
	    }
	}
	if (item.getGroupSid().size() == 0) {
	    EntityItemStringType groupSidType = Factories.sc.core.createEntityItemStringType();
	    groupSidType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getGroupSid().add(groupSidType);
	}
	return item;
    }
}
