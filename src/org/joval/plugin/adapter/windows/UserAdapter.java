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
import oval.schemas.definitions.windows.UserObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.UserItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.identity.ActiveDirectory;
import org.joval.os.windows.identity.LocalDirectory;
import org.joval.os.windows.identity.User;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates User OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserAdapter implements IAdapter {
    private IWmiProvider wmi;
    private String hostname;

    protected LocalDirectory local = null;
    protected ActiveDirectory ad = null;

    public UserAdapter(LocalDirectory local, ActiveDirectory ad, IWmiProvider wmi) {
	this.local = local;
	this.ad = ad;
	this.wmi = wmi;
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return UserObject.class;
    }

    public boolean connect() {
	if (wmi.connect()) {
	    return true;
	}
	return false;
    }

    public void disconnect() {
	wmi.disconnect();
	local = null;
	ad = null;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	UserObject uObj = (UserObject)rc.getObject();
	OperationEnumeration op = uObj.getUser().getOperation();
	String user = (String)uObj.getUser().getValue();

	try {
	    switch(op) {
	      case EQUALS:
		if (local.isMember(user)) {
		    items.add(makeItem(local.queryUser(user)));
		} else if (ad.isMember(user)) {
		    items.add(makeItem(ad.queryUser(user)));
		} else {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.WARNING);
		    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_AD_DOMAIN_UNKNOWN, user);
		    JOVALSystem.getLogger().warn(s);
		    msg.setValue(s);
		    rc.addMessage(msg);
		}
		break;
    
	      case NOT_EQUAL:
		for (User u : local.queryAllUsers()) {
		    if (!local.getQualifiedNetbiosName(user).equals(u.getNetbiosName())) {
			items.add(makeItem(u));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(user);
		    for (User u : local.queryAllUsers()) {
			Matcher m = null;
			if (local.isMember(u.getNetbiosName())) {
			    m = p.matcher(u.getName());
			} else {
			    m = p.matcher(u.getNetbiosName());
			}
			if (m.find()) {
			    items.add(makeItem(u));
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

    private JAXBElement<? extends ItemType> makeItem(User user) {
	UserItem item = JOVALSystem.factories.sc.windows.createUserItem();
	EntityItemStringType userType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	if (local.isBuiltinUser(user.getNetbiosName())) {
	    userType.setValue(user.getName());
	} else {
	    userType.setValue(user.getNetbiosName());
	}
	item.setUser(userType);
	EntityItemBoolType enabledType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	enabledType.setValue(user.isEnabled() ? "true" : "false");
	enabledType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setEnabled(enabledType);
	Collection<String> groupNetbiosNames = user.getGroupNetbiosNames();
	if (groupNetbiosNames.size() == 0) {
	    EntityItemStringType groupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    groupType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getGroup().add(groupType);
	} else {
	    for (String groupName : groupNetbiosNames) {
		EntityItemStringType groupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		if (local.isBuiltinGroup(groupName)) {
		    groupType.setValue(local.getName(groupName));
		} else {
		    groupType.setValue(groupName);
		}
		item.getGroup().add(groupType);
	    }
	}
	return JOVALSystem.factories.sc.windows.createUserItem(item);
    }
}
