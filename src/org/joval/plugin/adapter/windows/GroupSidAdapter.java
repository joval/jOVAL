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
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.identity.ActiveDirectory;
import org.joval.os.windows.identity.LocalDirectory;
import org.joval.os.windows.identity.Group;
import org.joval.os.windows.identity.User;
import org.joval.os.windows.wmi.WmiException;
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
    public GroupSidAdapter(LocalDirectory local, ActiveDirectory ad, IWmiProvider wmi) {
	super(local, ad, wmi);
    }

    /**
     * @override
     */
    public Class getObjectClass() {
	return GroupSidObject.class;
    }

    /**
     * @override
     */
    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	OperationEnumeration op = ((GroupSidObject)rc.getObject()).getGroupSid().getOperation();
	String groupSid = (String)((GroupSidObject)rc.getObject()).getGroupSid().getValue();

	try {
	    switch(op) {
	      case EQUALS:
		try {
		    items.add(makeItem(local.queryGroupBySid(groupSid)));
		} catch (NoSuchElementException e) {
		    items.add(makeItem(ad.queryGroupBySid(groupSid)));
		}
		break;
    
	      case NOT_EQUAL:
		for (Group g : local.queryAllGroups()) {
		    if (!groupSid.equals(g.getSid())) {
			items.add(makeItem(g));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(groupSid);
		    for (Group g : local.queryAllGroups()) {
			if (p.matcher(g.getSid()).find()) {
			    items.add(makeItem(g));
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		    rc.addMessage(msg);
		    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		break;
    
	      default:
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
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

    private JAXBElement<? extends ItemType> makeItem(Group group) {
	GroupSidItem item = JOVALSystem.factories.sc.windows.createGroupSidItem();
	EntityItemStringType groupSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	groupSidType.setValue(group.getSid());
	item.setGroupSid(groupSidType);
	for (String userNetbiosName : group.getMemberUserNetbiosNames()) {
	    User user = null;
	    try {
		if (local.isMember(userNetbiosName)) {
		    user = local.queryUser(userNetbiosName);
		} else if (ad.isMember(userNetbiosName)) {
		    user = ad.queryUser(userNetbiosName);
		}
		if (user != null) {
		    EntityItemStringType userSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    userSidType.setValue(user.getSid());
		    item.getUserSid().add(userSidType);
		}
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
	    Group subgroup = null;
	    try {
		if (local.isMember(groupNetbiosName)) {
		    subgroup = local.queryGroup(groupNetbiosName);
		} else if (ad.isMember(groupNetbiosName)) {
		    subgroup = ad.queryGroup(groupNetbiosName);
		}
		if (subgroup != null) {
		    EntityItemStringType subgroupSidType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    subgroupSidType.setValue(subgroup.getSid());
		    item.getSubgroupSid().add(subgroupSidType);
		}
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
