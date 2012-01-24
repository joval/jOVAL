// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.util.Hashtable;
import java.util.Collection;
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
import oval.schemas.definitions.windows.GroupSidObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.GroupSidItem;

import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates Group OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GroupSidAdapter extends UserAdapter {
    public GroupSidAdapter(IWindowsSession session) {
	super(session);
    }

    private static Class[] objectClasses = {GroupSidObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    /**
     * @override
     */
    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc)
	    throws NotCollectableException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	OperationEnumeration op = ((GroupSidObject)rc.getObject()).getGroupSid().getOperation();
	String groupSid = (String)((GroupSidObject)rc.getObject()).getGroupSid().getValue();

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
		    Pattern p = Pattern.compile(groupSid);
		    for (IGroup g : directory.queryAllGroups()) {
			if (p.matcher(g.getSid()).find()) {
			    items.add(makeItem(g));
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
		throw new NotCollectableException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
	    }
	} catch (NoSuchElementException e) {
	    // No match.
	} catch (WmiException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private JAXBElement<? extends ItemType> makeItem(IGroup group) {
	GroupSidItem item = JOVALSystem.factories.sc.windows.createGroupSidItem();
	EntityItemStringType groupSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	groupSidType.setValue(group.getSid());
	item.setGroupSid(groupSidType);
	for (String userNetbiosName : group.getMemberUserNetbiosNames()) {
	    try {
		IUser user = directory.queryUser(userNetbiosName);
		EntityItemStringType userSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		userSidType.setValue(user.getSid());
		item.getUserSid().add(userSidType);
	    } catch (IllegalArgumentException e) {
	    } catch (NoSuchElementException e) {
	    } catch (WmiException e) {
	    }
	}
	if (item.getUserSid().size() == 0) {
	    EntityItemStringType userSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    userSidType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getUserSid().add(userSidType);
	}
	for (String groupNetbiosName : group.getMemberGroupNetbiosNames()) {
	    try {
		IGroup subgroup = directory.queryGroup(groupNetbiosName);
		EntityItemStringType subgroupSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		subgroupSidType.setValue(subgroup.getSid());
		item.getSubgroupSid().add(subgroupSidType);
	    } catch (IllegalArgumentException e) {
	    } catch (NoSuchElementException e) {
	    } catch (WmiException e) {
	    }
	}
	if (item.getSubgroupSid().size() == 0) {
	    EntityItemStringType subgroupSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    subgroupSidType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getSubgroupSid().add(subgroupSidType);
	}
	return JOVALSystem.factories.sc.windows.createGroupSidItem(item);
    }
}
