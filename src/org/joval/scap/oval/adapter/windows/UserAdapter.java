// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
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
import oval.schemas.definitions.windows.UserObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.UserItem;
import oval.schemas.results.core.ResultEnumeration;

import jsaf.intf.system.IBaseSession;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IUser;
import jsaf.intf.windows.identity.IGroup;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.wmi.ISWbemObject;
import jsaf.intf.windows.wmi.ISWbemProperty;
import jsaf.intf.windows.wmi.ISWbemPropertySet;
import jsaf.intf.windows.wmi.IWmiProvider;
import jsaf.provider.windows.Timestamp;
import jsaf.provider.windows.wmi.WmiException;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:user_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserAdapter implements IAdapter {
    protected IWindowsSession session;
    protected IDirectory directory;
    private Hashtable<String, Date> logons;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(UserObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	if (logons == null) {
	    initLogons();
	}
	Collection<UserItem> items = new Vector<UserItem>();
	UserObject uObj = (UserObject)obj;
	OperationEnumeration op = uObj.getUser().getOperation();
	Collection<String> users = new Vector<String>();
	try {
	    String user = (String)uObj.getUser().getValue();
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

    private void initLogons() {
	logons = new Hashtable<String, Date>();
	try {
	    String wql = "select * from Win32_NetworkLoginProfile";
	    for (ISWbemObject obj : session.getWmiProvider().execQuery(IWmiProvider.CIMv2, wql)) {
		ISWbemPropertySet props = obj.getProperties();
		String name = props.getItem("Name").getValueAsString();
		BigInteger timestamp = props.getItem("LastLogon").getValueAsTimestamp();
		if (timestamp != null) {
		    logons.put(name, new Date(Timestamp.getTime(timestamp)));
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private UserItem makeItem(IUser user) throws WmiException {
	UserItem item = Factories.sc.windows.createUserItem();
	EntityItemStringType userType = Factories.sc.core.createEntityItemStringType();
	if (user.isBuiltin()) {
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
	if (groupNetbiosNames == null || groupNetbiosNames.size() == 0) {
	    EntityItemStringType groupType = Factories.sc.core.createEntityItemStringType();
	    groupType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    item.getGroup().add(groupType);
	} else {
	    for (String groupName : groupNetbiosNames) {
		IGroup group = directory.queryGroup(groupName);
		EntityItemStringType groupType = Factories.sc.core.createEntityItemStringType();
		if (group.isBuiltin()) {
		    groupType.setValue(group.getName());
		} else {
		    groupType.setValue(groupName);
		}
		item.getGroup().add(groupType);
	    }
	}
	EntityItemIntType lastLogonType = Factories.sc.core.createEntityItemIntType();
	if (logons.containsKey(user.getNetbiosName())) {
	    lastLogonType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    long secs = logons.get(user.getNetbiosName()).getTime()/1000L;
	    lastLogonType.setValue(Long.toString(secs));
	} else {
	    lastLogonType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setLastLogon(lastLogonType);
	return item;
    }
}
