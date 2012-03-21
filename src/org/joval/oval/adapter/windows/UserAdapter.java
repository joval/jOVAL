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
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.UserItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * Evaluates User OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserAdapter implements IAdapter {
    protected IWindowsSession session;
    protected IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(UserObject.class);
	}
	return classes;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectException, OvalException {
	directory = session.getDirectory();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	UserObject uObj = (UserObject)rc.getObject();
	OperationEnumeration op = uObj.getUser().getOperation();
	Collection<String> users = new Vector<String>();
	try {
	    if (uObj.getUser().isSetVarRef()) {
		users = rc.resolve(uObj.getUser().getVarRef());
	    } else {
		users.add((String)uObj.getUser().getValue());
	    }

	    for (String user : users) {
		switch(op) {
		  case EQUALS:
		    try {
			items.add(makeItem(directory.queryUser(user)));
		    } catch (IllegalArgumentException e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.WARNING);
			String s = JOVALMsg.getMessage(JOVALMsg.ERROR_AD_DOMAIN_UNKNOWN, user);
			session.getLogger().warn(s);
			msg.setValue(s);
			rc.addMessage(msg);
		    }
		    break;

		  case NOT_EQUAL:
		    for (IUser u : directory.queryAllUsers()) {
			if (!directory.getQualifiedNetbiosName(user).equals(u.getNetbiosName())) {
			    items.add(makeItem(u));
			}
		    }
		    break;

		  case PATTERN_MATCH:
		    try {
			Pattern p = Pattern.compile(user);
			for (IUser u : directory.queryAllUsers()) {
			    Matcher m = null;
			    if (directory.isLocal(u.getNetbiosName())) {
				m = p.matcher(u.getName());
			    } else {
				m = p.matcher(u.getNetbiosName());
			    }
			    if (m.find()) {
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

    private JAXBElement<? extends ItemType> makeItem(IUser user) {
	UserItem item = Factories.sc.windows.createUserItem();
	EntityItemStringType userType = Factories.sc.core.createEntityItemStringType();
	if (directory.isBuiltinUser(user.getNetbiosName())) {
	    userType.setValue(user.getName());
	} else {
	    userType.setValue(user.getNetbiosName());
	}
	item.setUser(userType);
	EntityItemBoolType enabledType = Factories.sc.core.createEntityItemBoolType();
	enabledType.setValue(user.isEnabled() ? "true" : "false");
	enabledType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setEnabled(enabledType);
	Collection<String> groupNetbiosNames = user.getGroupNetbiosNames();
	if (groupNetbiosNames.size() == 0) {
	    EntityItemStringType groupType = Factories.sc.core.createEntityItemStringType();
	    groupType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getGroup().add(groupType);
	} else {
	    for (String groupName : groupNetbiosNames) {
		EntityItemStringType groupType = Factories.sc.core.createEntityItemStringType();
		if (directory.isBuiltinGroup(groupName)) {
		    groupType.setValue(directory.getName(groupName));
		} else {
		    groupType.setValue(groupName);
		}
		item.getGroup().add(groupType);
	    }
	}
	return Factories.sc.windows.createUserItem(item);
    }
}
