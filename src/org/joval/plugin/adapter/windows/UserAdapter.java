// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
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
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

/**
 * Evaluates User OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserAdapter implements IAdapter {
    static final String ROOT_NS	= "root\\cimv2";
    static final String USER_WQL			= "SELECT * FROM Win32_UserAccount";
    static final String USER_WQL_DOMAIN_CONDITION	= "Domain='$domain'";
    static final String USER_WQL_LOCAL_CONDITION	= "LocalAccount=TRUE";
    static final String USER_WQL_NAME_CONDITION		= "Name='$username'";

    static final String GROUP_WQL			= "ASSOCIATORS OF {$conditions} WHERE resultClass=Win32_Group";
    static final String DOMAIN_ASSOC_CONDITION		= "Win32_UserAccount.Domain=\"$domain\"";
    static final String LOCAL_ASSOC_CONDITION		= "Win32_UserAccount.LocalAccount=TRUE";
    static final String NAME_ASSOC_CONDITION		= "Name=\"$username\"";

    private IWmiProvider wmi;
    private Hashtable<String, UserItem> users;
    private boolean preloaded = false;

    public UserAdapter(IWmiProvider wmi) {
	this.wmi = wmi;
	users = new Hashtable<String, UserItem>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return UserObject.class;
    }

    public boolean connect() {
	return wmi.connect();
    }

    public void disconnect() {
	wmi.disconnect();
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	UserObject uObj = (UserObject)rc.getObject();
	String user = (String)uObj.getUser().getValue();

	try {
	    switch(uObj.getUser().getOperation()) {
	      case EQUALS:
		items.add(JOVALSystem.factories.sc.windows.createUserItem(queryUser(user)));
		break;
    
	      case NOT_EQUAL:
		for (UserItem item : queryAllUsers()) {
		    if (!user.equals((String)item.getUser().getValue())) {
			items.add(JOVALSystem.factories.sc.windows.createUserItem(item));
		    }
		}
		break;
    
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(user);
		    for (UserItem item : queryAllUsers()) {
			if (p.matcher((String)item.getUser().getValue()).find()) {
			    items.add(JOVALSystem.factories.sc.windows.createUserItem(item));
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
		    rc.addMessage(msg);
		    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		break;
    
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", uObj.getUser().getOperation()));
	    }
	} catch (WmiException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALSystem.getMessage("ERROR_WINWMI_GENERAL", e.getMessage()));
	    rc.addMessage(msg);
	}
	if (items.size() == 0) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALSystem.getMessage("STATUS_NOT_FOUND", user, uObj.getId()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    /**
     * Get the Domain portion of a Domain\\User String.
     */
    private String getDomain(String s) {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    return null;
	} else {
	    return s.substring(0, ptr);
	}
    }

    /**
     * Get the User portion of a Domain\\User String.  If there is no domain portion, returns the original String.
     */
    private String getUser(String s) {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    return s;
	} else {
	    return s.substring(ptr+1);
	}
    }

    private UserItem queryUser(String name) throws WmiException {
	UserItem item = users.get(name.toUpperCase());
	if (item == null) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(USER_WQL_NAME_CONDITION.replaceAll("(?i)\\$username", Matcher.quoteReplacement(getUser(name))));
	    wql.append(" AND ");
	    String domain = getDomain(name);
	    if (domain == null) {
		wql.append(USER_WQL_LOCAL_CONDITION);
	    } else {
		wql.append(USER_WQL_DOMAIN_CONDITION.replaceAll("(?i)\\$username", Matcher.quoteReplacement(domain)));
	    }

	    ISWbemObjectSet os = wmi.execQuery(ROOT_NS, wql.toString());
	    if (os.getSize() == 0) {
		item = JOVALSystem.factories.sc.windows.createUserItem();
		EntityItemStringType user = JOVALSystem.factories.sc.core.createEntityItemStringType();
		user.setValue(name);
		item.setUser(user);
		item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		ISWbemPropertySet columns = os.iterator().next().getProperties();
		boolean enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
		item = makeItem(getUser(name), domain, enabled);
	    }
	    users.put(name.toUpperCase(), item);
	}
	return item;
    }

    private List<UserItem> queryAllUsers() throws WmiException {
	if (!preloaded) {
	    for (ISWbemObject row : wmi.execQuery(ROOT_NS, USER_WQL)) {
		ISWbemPropertySet columns = row.getProperties();
		String username = columns.getItem("Name").getValueAsString();
		String domain = columns.getItem("Domain").getValueAsString();
		boolean enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
		String s = domain + "\\" + username;
		users.put(s.toUpperCase(), makeItem(username, domain, enabled));
	    }
	    preloaded = true;
	}
	List<UserItem> items = new Vector<UserItem>();
	for (UserItem item : users.values()) {
	    items.add(item);
	}
	return items;
    }

    private UserItem makeItem(String username, String domain, boolean enabled) throws WmiException {
System.out.println("DAS: makeItem username=" + username + " domain=" + domain + ", enabled=" + enabled);
	UserItem item = JOVALSystem.factories.sc.windows.createUserItem();
	EntityItemStringType user = JOVALSystem.factories.sc.core.createEntityItemStringType();
	if (domain == null) {
	    user.setValue(username);
	} else {
	    user.setValue(domain + "\\" + username);
	}
	item.setUser(user);
	EntityItemBoolType enabledType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	enabledType.setValue(enabled ? "true" : "false");
	enabledType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setEnabled(enabledType);

	if (enabled) {
	    StringBuffer conditions = new StringBuffer();
	    if (domain == null) {
	        conditions.append(LOCAL_ASSOC_CONDITION);
	    } else {
	        conditions.append(DOMAIN_ASSOC_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));
	    }
	    conditions.append(",");
	    conditions.append(NAME_ASSOC_CONDITION.replaceAll("(?i)\\$username", Matcher.quoteReplacement(username)));
	    String wql = GROUP_WQL.replaceAll("(?i)\\$conditions", Matcher.quoteReplacement(conditions.toString()));
System.out.println(wql);
	    for (ISWbemObject row : wmi.execQuery(ROOT_NS, wql)) {
		ISWbemPropertySet columns = row.getProperties();
		EntityItemStringType group = JOVALSystem.factories.sc.core.createEntityItemStringType();
		group.setValue(columns.getItem("Domain").getValueAsString()+"\\"+columns.getItem("Name").getValueAsString());
		item.getGroup().add(group);
	    }
	}

	return item;
    }
}
