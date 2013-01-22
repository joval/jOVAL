// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.macos.AccountinfoObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.macos.AccountinfoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.macos.DsclTool;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves AccountinfoItems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AccountinfoAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, AccountinfoItem> infoMap = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session) {
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
	Collection<AccountinfoItem> items = new Vector<AccountinfoItem>();
	try {
	    init();
	    AccountinfoObject aObj = (AccountinfoObject)obj;
	    String username = (String)aObj.getUsername().getValue();
	    OperationEnumeration op = aObj.getUsername().getOperation();
	    switch(op) {
	      case EQUALS:
		if (infoMap.containsKey(username)) {
		    items.add(infoMap.get(username));
		}
		break;

	      case NOT_EQUAL:
		for (String s : infoMap.keySet()) {
		    if (!s.equals(username)) {
			items.add(infoMap.get(s));
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = Pattern.compile(username);
		for (String s : infoMap.keySet()) {
		    if (p.matcher(s).find()) {
			items.add(infoMap.get(s));
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    /**
     * Adapter initialization (idempotent).
     */
    private void init() throws Exception {
	if (infoMap == null) {
	    DsclTool tool = new DsclTool(session);
	    infoMap = new HashMap<String, AccountinfoItem>();
	    for (String username : tool.getUsers()) {
		AccountinfoItem item = Factories.sc.macos.createAccountinfoItem();
    
		EntityItemStringType usernameType = Factories.sc.core.createEntityItemStringType();
		usernameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		usernameType.setValue(username);
		item.setUsername(usernameType);
   
		StringBuffer sb = new StringBuffer("dscl localhost -read Search/Users/");
		sb.append(username);
		sb.append(" passwd uid gid realname home shell");
		HashMap<String, String> attrs = new HashMap<String, String>();
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
		    } else if (lastKey != null && !attrs.containsKey(lastKey)) {
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
    
		infoMap.put(username, item);
	    }
	}
    }
}
