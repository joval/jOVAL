// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IGroup;
import jsaf.intf.windows.identity.IUser;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.wmi.WmiException;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.GroupObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.GroupItem;

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
public class GroupAdapter extends UserAdapter {
    // Implement IAdapter

    @Override
    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(GroupObject.class);
	} else {
	    notapplicable.add(GroupObject.class);
	}
	return classes;
    }

    @Override
    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	Collection<GroupItem> items = new ArrayList<GroupItem>();
	OperationEnumeration op = ((GroupObject)obj).getGroup().getOperation();
	String group = (String)((GroupObject)obj).getGroup().getValue();
	try {
	    switch(op) {
	      case EQUALS:
		try {
		    items.add(makeItem(directory.queryGroup(group)));
		} catch (IllegalArgumentException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.WARNING);
		    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_AD_DOMAIN_UNKNOWN, group);
		    session.getLogger().warn(s);
		    msg.setValue(s);
		    rc.addMessage(msg);
		}
		break;
    
	      case NOT_EQUAL:
		for (IGroup g : directory.queryAllGroups()) {
		    if (!directory.getQualifiedNetbiosName(group).equals(g.getNetbiosName())) {
			items.add(makeItem(g));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(group);
		    for (IGroup g : directory.queryAllGroups()) {
			Matcher m = null;
			if (directory.isLocal(g.getNetbiosName())) {
			    m = p.matcher(g.getName());
			} else {
			    m = p.matcher(g.getNetbiosName());
			}
			if (m.find()) {
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
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private GroupItem makeItem(IGroup group) throws WmiException {
	GroupItem item = Factories.sc.windows.createGroupItem();
	EntityItemStringType groupType = Factories.sc.core.createEntityItemStringType();
	if (group.isBuiltin()) {
	    groupType.setValue(group.getName());
	} else {
	    groupType.setValue(group.getNetbiosName());
	}
	item.setGroup(groupType);
	Collection<String> userNetbiosNames = group.getMemberUserNetbiosNames();
	if (userNetbiosNames.size() == 0) {
	    EntityItemStringType userType = Factories.sc.core.createEntityItemStringType();
	    userType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getUser().add(userType);
	} else {
	    for (String userNetbiosName : userNetbiosNames) {
		IUser user = directory.queryUser(userNetbiosName);
		EntityItemStringType userType = Factories.sc.core.createEntityItemStringType();
		if (user.isBuiltin()) {
		    userType.setValue(user.getName());
		} else {
		    userType.setValue(userNetbiosName);
		}
		item.getUser().add(userType);
	    }
	}
	Collection<String> groupNetbiosNames = group.getMemberGroupNetbiosNames();
	if (groupNetbiosNames.size() == 0) {
	    EntityItemStringType subgroupType = Factories.sc.core.createEntityItemStringType();
	    subgroupType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getSubgroup().add(subgroupType);
	} else {
	    for (String groupNetbiosName : groupNetbiosNames) {
		IGroup subgroup = directory.queryGroup(groupNetbiosName);
		EntityItemStringType subgroupType = Factories.sc.core.createEntityItemStringType();
		if (subgroup.isBuiltin()) {
		    subgroupType.setValue(subgroup.getName());
		} else {
		    subgroupType.setValue(groupNetbiosName);
		}
		item.getSubgroup().add(subgroupType);
	    }
	}
	return item;
    }
}
