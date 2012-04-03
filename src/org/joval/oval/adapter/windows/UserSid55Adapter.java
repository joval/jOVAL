// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.util.Hashtable;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.UserSid55Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.UserSidItem;

import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * Collects items for the user_sid55_object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserSid55Adapter extends UserAdapter {
    // Implement IAdapter

    @Override
    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(UserSid55Object.class);
	}
	return classes;
    }

    @Override
    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException, OvalException {
	directory = session.getDirectory();
	Collection<UserSidItem> items = new Vector<UserSidItem>();
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
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage()));
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
	for (String groupNetbiosName : user.getGroupNetbiosNames()) {
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
	if (item.getGroupSid().size() == 0) {
	    EntityItemStringType groupSidType = Factories.sc.core.createEntityItemStringType();
	    groupSidType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getGroupSid().add(groupSidType);
	}
	return item;
    }
}
