// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.macos.AccountinfoObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.macos.AccountinfoItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.macos.DsclTool;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Retrieves AccountinfoItems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AccountinfoAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, AccountinfoItem> accountinfo = null;
    private MessageType error = null;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    if (((IUnixSession)session).getFlavor() == IUnixSession.Flavor.MACOSX) {
		this.session = (IUnixSession)session;
		classes.add(AccountinfoObject.class);
	    }
	}
	return classes;
    }

    public Collection<AccountinfoItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (accountinfo == null && error == null) {
	    try {
		createAccountInfo();
	    } catch (Exception e) {
		error = Factories.common.createMessageType();
		error.setLevel(MessageLevelEnumeration.ERROR);
		error.setValue(e.getMessage());
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}

	Collection<AccountinfoItem> items = new Vector<AccountinfoItem>();
	if (accountinfo != null) {
	    AccountinfoObject aObj = (AccountinfoObject)obj;
	    String username = (String)aObj.getUsername().getValue();
	    OperationEnumeration op = aObj.getUsername().getOperation();
	    switch(op) {
	      case EQUALS:
		if (accountinfo.containsKey(username)) {
		    items.add(accountinfo.get(username));
		}
		break;

	      case NOT_EQUAL:
		for (String s : accountinfo.keySet()) {
		    if (!s.equals(username)) {
			items.add(accountinfo.get(s));
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = Pattern.compile(username);
		for (String s : accountinfo.keySet()) {
		    if (p.matcher(s).find()) {
			items.add(accountinfo.get(s));
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	if (error != null) {
	    rc.addMessage(error);
	}
	return items;
    }

    // Private

    void createAccountInfo() throws Exception {
	DsclTool tool = new DsclTool(session);
	accountinfo = new Hashtable<String, AccountinfoItem>();
	for (String username : tool.getUsers()) {
	    AccountinfoItem item = Factories.sc.macos.createAccountinfoItem();

	    EntityItemStringType usernameType = Factories.sc.core.createEntityItemStringType();
	    usernameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    usernameType.setValue(username);
	    item.setUsername(usernameType);

	    StringBuffer sb = new StringBuffer("dscl . -read /Users/");
	    sb.append(username);
	    sb.append(" passwd uid gid realname home shell");
	    Hashtable<String, String> attrs = new Hashtable<String, String>();
	    List<String> lines = SafeCLI.multiLine(sb.toString(), session, IUnixSession.Timeout.S);
	    String lastKey = null;
	    for (int i=0; i < lines.size(); i++) {
		String line = lines.get(i).trim();
		if (line.startsWith("dsAttrTypeNative:")) {
		    int ptr = line.indexOf(":", 17);
		    if (ptr != -1) {
			String key = line.substring(17, ptr);
			ptr = ptr + 1;
			if (line.length() > ptr) {
			    String val = line.substring(ptr).trim();
			    attrs.put(key, val);
			}
			lastKey = key;
		    }
		} else if (lastKey != null && !attrs.contains(lastKey)) {
		    attrs.put(lastKey, line);
		}
	    }

	    if (attrs.containsKey("gid")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(attrs.get("gid"));
		item.setGid(type);
	    }

	    if (attrs.containsKey("home")) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		type.setValue(attrs.get("home"));
		item.setHomeDir(type);
	    }

	    if (attrs.containsKey("shell")) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		type.setValue(attrs.get("shell"));
		item.setLoginShell(type);
	    }

	    if (attrs.containsKey("passwd")) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		type.setValue(attrs.get("passwd"));
		item.setPassword(type);
	    }

	    if (attrs.containsKey("realname")) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		type.setValue(attrs.get("realname"));
		item.setRealname(type);
	    }

	    if (attrs.containsKey("uid")) {
		EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
		type.setDatatype(SimpleDatatypeEnumeration.INT.value());
		type.setValue(attrs.get("uid"));
		item.setUid(type);
	    }

	    accountinfo.put(username, item);
	}
    }

}
