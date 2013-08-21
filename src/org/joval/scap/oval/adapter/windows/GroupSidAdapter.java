// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.identity.IdentityException;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IGroup;
import jsaf.intf.windows.identity.IUser;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.GroupSidObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.GroupSidItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Group OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GroupSidAdapter implements IAdapter {
    private IWindowsSession session;
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(GroupSidObject.class);
	} else {
	    notapplicable.add(GroupSidObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	Collection<GroupSidItem> items = new ArrayList<GroupSidItem>();
	OperationEnumeration op = ((GroupSidObject)obj).getGroupSid().getOperation();
	String groupSid = (String)((GroupSidObject)obj).getGroupSid().getValue();

	try {
	    switch(op) {
	      case EQUALS:
		items.add(makeItem(directory.queryGroupBySid(groupSid)));
		break;
    
	      case NOT_EQUAL:
		for (IGroup g : directory.queryAllGroups()) {
		    if (!groupSid.equals(g.getSid())) {
			items.add(makeItem(g));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = StringTools.pattern(groupSid);
		    for (IGroup g : directory.queryAllGroups()) {
			if (p.matcher(g.getSid()).find()) {
			    items.add(makeItem(g));
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
	} catch (IdentityException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_IDENTITY, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private GroupSidItem makeItem(IGroup group) throws IdentityException {
	GroupSidItem item = Factories.sc.windows.createGroupSidItem();
	EntityItemStringType groupSidType = Factories.sc.core.createEntityItemStringType();
	groupSidType.setValue(group.getSid());
	item.setGroupSid(groupSidType);
	for (String userNetbiosName : group.getMemberUserNetbiosNames()) {
	    try {
		IUser user = directory.queryUser(userNetbiosName);
		EntityItemStringType userSidType = Factories.sc.core.createEntityItemStringType();
		userSidType.setValue(user.getSid());
		item.getUserSid().add(userSidType);
	    } catch (IllegalArgumentException e) {
	    } catch (NoSuchElementException e) {
	    }
	}
	if (item.getUserSid().size() == 0) {
	    EntityItemStringType userSidType = Factories.sc.core.createEntityItemStringType();
	    userSidType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getUserSid().add(userSidType);
	}
	for (String groupNetbiosName : group.getMemberGroupNetbiosNames()) {
	    try {
		IGroup subgroup = directory.queryGroup(groupNetbiosName);
		EntityItemStringType subgroupSidType = Factories.sc.core.createEntityItemStringType();
		subgroupSidType.setValue(subgroup.getSid());
		item.getSubgroupSid().add(subgroupSidType);
	    } catch (IllegalArgumentException e) {
	    } catch (NoSuchElementException e) {
	    }
	}
	if (item.getSubgroupSid().size() == 0) {
	    EntityItemStringType subgroupSidType = Factories.sc.core.createEntityItemStringType();
	    subgroupSidType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getSubgroupSid().add(subgroupSidType);
	}
	return item;
    }
}
