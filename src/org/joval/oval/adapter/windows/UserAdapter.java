// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
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

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException, OvalException {
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
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage()));
	    rc.addMessage(msg);
	} catch (ParseException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    private void initLogons() {
	logons = new Hashtable<String, Date>();
	try {
	    String wql = "select LastLogon from Win32_NetworkLoginProfile";
	    for (ISWbemObject obj : session.getWmiProvider().execQuery(IWmiProvider.CIMv2, wql)) {
		String s = null;
		try {
		    ISWbemPropertySet props = obj.getProperties();
		    String name = props.getItem("Name").getValueAsString();
		    //
		    // Convert LastLogon string to number of seconds since 1970
		    //
		    s = props.getItem("LastLogon").getValueAsString();
		    if (s != null && s.length() > 21) {
			String tz = s.substring(21).trim();
			if (tz.length() == 4) {
			    char c = tz.charAt(0);
			    if (c == '-' || c == '+') {
				StringBuffer sb = new StringBuffer();
				sb.append(c);
				sb.append("0");
				sb.append(tz.substring(1));
				tz = sb.toString();
			    }
			}
			Date date = null;
			if (tz.length() == 5) {
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
			    date = sdf.parse(s.substring(0, 18) + tz);
			} else {
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
			    date = sdf.parse(s.substring(0, 18));
			}
			logons.put(name, date);
		    }
		} catch (ParseException e) {
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private UserItem makeItem(IUser user) throws WmiException, ParseException {
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
