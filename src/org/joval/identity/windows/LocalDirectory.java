// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity.windows;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.GroupItem;
import oval.schemas.systemcharacteristics.windows.UserItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

/**
 * The LocalDirectory class provides a mechanism to query the local User/Group directory for a Windows machine.  It is
 * case-insensitive, and it intelligently caches results so that subsequent requests for the same object can be returned
 * from memory.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalDirectory {
    private static final String USER_WQL		= "SELECT Name, Domain, Disabled FROM Win32_UserAccount";
    private static final String GROUP_WQL		= "SELECT Name, Domain FROM Win32_Group";
    private static final String DOMAIN_CONDITION	= "Domain='$domain'";
    private static final String LOCAL_CONDITION		= "LocalAccount=TRUE";
    private static final String NAME_CONDITION		= "Name='$name'";

    private static final String USER_GROUP_WQL		= "ASSOCIATORS OF {$conditions} WHERE resultClass=Win32_Group";
    private static final String USERDOM_ASSOC_CONDITION	= "Win32_UserAccount.Domain=\"$domain\"";
    private static final String NAME_ASSOC_CONDITION	= "Name=\"$username\"";

    private static final String GROUP_USER_WQL		= "ASSOCIATORS OF {$conditions} WHERE resultClass=Win32_UserAccount";
    private static final String DOMUSER_ASSOC_CONDITION	= "Win32_Group.Domain=\"$domain\"";

    private Hashtable<String, UserItem> users;
    private Hashtable<String, GroupItem> groups;

    private IWmiProvider wmi;
    private String hostname;
    private boolean preloadedUsers = false;
    private boolean preloadedGroups = false;

    public LocalDirectory(String hostname, IWmiProvider wmi) {
	this.wmi = wmi;
	this.hostname = hostname.toUpperCase();
	users = new Hashtable<String, UserItem>();
	groups = new Hashtable<String, GroupItem>();
    }

    /**
     * Check whether the supplied name is supposed to indicate a machine-local user or group.
     */
    public boolean isLocal(String name) {
	String domain = getDomain(name);
	if (domain == null) {
	    return true;
	} else {
	    return hostname.equals(domain.toUpperCase());
	}
    }

    /**
     * Query for an individual user.  The name parameter should be of the form DOMAIN\\username.  For built-in accounts,
     * the DOMAIN\\ part can be dropped, in which case the name parameter is just the username.
     *
     * If the user is not found, then a UserItem is returned with a status of DOES_NOT_EXIST.
     */
    public UserItem queryUser(String name) throws WmiException {
	UserItem item = users.get(name.toUpperCase());
	if (item == null) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(NAME_CONDITION.replaceAll("(?i)\\$name", Matcher.quoteReplacement(getUser(name))));
	    wql.append(" AND ");
	    String domain = getDomain(name);
	    if (domain == null) {
		wql.append(LOCAL_CONDITION);
	    } else {
		wql.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));
	    }

	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql.toString());
	    if (os.getSize() == 0) {
		item = JOVALSystem.factories.sc.windows.createUserItem();
		EntityItemStringType user = JOVALSystem.factories.sc.core.createEntityItemStringType();
		user.setValue(name);
		item.setUser(user);
		item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		ISWbemPropertySet columns = os.iterator().next().getProperties();
		boolean enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
		if (domain == null) {
		    domain = columns.getItem("Domain").getValueAsString();
		    name = domain + "\\" + columns.getItem("Name").getValueAsString();
		    item = users.get(name.toUpperCase()); // check again using the complete name
		}
		if (item == null) {
		    item = makeUserItem(name, enabled);
		}
	    }
	    users.put(name.toUpperCase(), item);
	}
	return item;
    }

    /**
     * Returns a List of all the local users.
     */
    public List<UserItem> queryAllUsers() throws WmiException {
	if (!preloadedUsers) {
	    for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, USER_WQL)) {
		ISWbemPropertySet columns = row.getProperties();
		String username = columns.getItem("Name").getValueAsString();
		String domain = columns.getItem("Domain").getValueAsString();
		boolean enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
		String s = domain + "\\" + username;
		if (users.get(s.toUpperCase()) == null) {
		    users.put(s.toUpperCase(), makeUserItem(s, enabled));
		}
	    }
	    preloadedUsers = true;
	}
	List<UserItem> items = new Vector<UserItem>();
	for (UserItem item : users.values()) {
	    items.add(item);
	}
	return items;
    }

    /**
     * Query for an individual group.  The name parameter should be of the form DOMAIN\\username.  For built-in groups,
     * the DOMAIN\\ part can be dropped, in which case the name parameter is just the group name.
     *
     * If the group is not found, then a GroupItem is returned with a status of DOES_NOT_EXIST.
     */
    public GroupItem queryGroup(String name) throws WmiException {
	GroupItem item = groups.get(name.toUpperCase());
	if (item == null) {
	    StringBuffer wql = new StringBuffer(GROUP_WQL);
	    wql.append(" WHERE ");
	    wql.append(NAME_CONDITION.replaceAll("(?i)\\$name", Matcher.quoteReplacement(getUser(name))));
	    wql.append(" AND ");
	    String domain = getDomain(name);
	    if (domain == null) {
		wql.append(LOCAL_CONDITION);
	    } else {
		wql.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));
	    }

	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql.toString());
	    if (os.getSize() == 0) {
		item = JOVALSystem.factories.sc.windows.createGroupItem();
		EntityItemStringType group = JOVALSystem.factories.sc.core.createEntityItemStringType();
		group.setValue(name);
		item.setGroup(group);
		item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		ISWbemPropertySet columns = os.iterator().next().getProperties();
		if (domain == null) {
		    domain = columns.getItem("Domain").getValueAsString();
		    name = domain + "\\" + columns.getItem("Name").getValueAsString();
		    item = groups.get(name.toUpperCase()); // check again using the complete name
		}
		if (item == null) {
		    item = makeGroupItem(name);
		}
	    }
	    groups.put(name.toUpperCase(), item);
	}
	return item;
    }

    /**
     * Returns a List of all the local groups.
     */
    public List<GroupItem> queryAllGroups() throws WmiException {
	if (!preloadedGroups) {
	    StringBuffer wql = new StringBuffer(GROUP_WQL);
	    wql.append(" WHERE ");
	    wql.append(LOCAL_CONDITION);
	    for (ISWbemObject rows : wmi.execQuery(IWmiProvider.CIMv2, wql.toString())) {
		ISWbemPropertySet columns = rows.getProperties();
		String group = columns.getItem("Name").getValueAsString();
		String domain = columns.getItem("Domain").getValueAsString();
		String s = domain + "\\" + group;
		if (groups.get(s.toUpperCase()) == null) {
		    groups.put(s.toUpperCase(), makeGroupItem(s));
		}
	    }
	    preloadedGroups = true;
	}
	List<GroupItem> items = new Vector<GroupItem>();
	for (GroupItem item : groups.values()) {
	    items.add(item);
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

    private boolean isBuiltinUser(String s) {
	String domain = getDomain(s);
	boolean local = false;
	if (domain == null) {
	    local = true;
	} else {
	    if (domain.toUpperCase().equals(hostname)) {
		local = true;
	    }
	}
	if (local) {
	    String username = getUser(s);
	    if ("Administrator".equals(username)) {
		return true;
	    } else if ("Guest".equals(username)) {
		return true;
	    } else if ("HomeGroupUser$".equals(username)) {
		return true;
	    }
	}
	return false;
    }

    private boolean isBuiltinGroup(String s) {
	String domain = getDomain(s);
	boolean local = false;
	if (domain == null) {
	    local = true;
	} else {
	    if (domain.toUpperCase().equals(hostname)) {
		local = true;
	    }
	}
	if (local) {
	    String group = getUser(s);
	    if ("Account Operators".equals(group)) {
		return true;
	    } else if ("Administrators".equals(group)) {
		return true;
	    } else if ("Backup Operators".equals(group)) {
		return true;
	    } else if ("Cryptographic Operators".equals(group)) {
		return true;
	    } else if ("Distributed COM Users".equals(group)) {
		return true;
	    } else if ("Event Log Readers".equals(group)) {
		return true;
	    } else if ("Guests".equals(group)) {
		return true;
	    } else if ("HelpLibraryUpdaters".equals(group)) {
		return true;
	    } else if ("HomeUsers".equals(group)) {
		return true;
	    } else if ("IIS_IUSRS".equals(group)) {
		return true;
	    } else if ("Network Configuration Operators".equals(group)) {
		return true;
	    } else if ("Performance Log Users".equals(group)) {
		return true;
	    } else if ("Performance Monitor Users".equals(group)) {
		return true;
	    } else if ("Power Users".equals(group)) {
		return true;
	    } else if ("Print Operators".equals(group)) {
		return true;
	    } else if ("Remote Desktop Users".equals(group)) {
		return true;
	    } else if ("Replicator".equals(group)) {
		return true;
	    } else if ("Server Operators".equals(group)) {
		return true;
	    } else if ("Users".equals(group)) {
		return true;
	    }
	}
	return false;
    }

    private UserItem makeUserItem(String name, boolean enabled) throws WmiException {
	UserItem item = JOVALSystem.factories.sc.windows.createUserItem();
	EntityItemStringType user = JOVALSystem.factories.sc.core.createEntityItemStringType();
	if (isBuiltinUser(name)) {
	    user.setValue(getUser(name));
	} else {
	    user.setValue(name);
	}
	item.setUser(user);
	EntityItemBoolType enabledType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	enabledType.setValue(enabled ? "true" : "false");
	enabledType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setEnabled(enabledType);

	StringBuffer conditions = new StringBuffer();
	conditions.append(USERDOM_ASSOC_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(getDomain(name))));
	conditions.append(",");
	conditions.append(NAME_ASSOC_CONDITION.replaceAll("(?i)\\$username", Matcher.quoteReplacement(getUser(name))));
	String wql = USER_GROUP_WQL.replaceAll("(?i)\\$conditions", Matcher.quoteReplacement(conditions.toString()));
	for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, wql)) {
	    ISWbemPropertySet columns = row.getProperties();
	    EntityItemStringType groupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    String domain = columns.getItem("Domain").getValueAsString();
	    String group = columns.getItem("Name").getValueAsString();
	    String s = domain + "\\" + group;
	    if (isBuiltinGroup(s)) {
		groupType.setValue(group);
	    } else {
		groupType.setValue(s);
	    }
	    item.getGroup().add(groupType);
	}

	return item;
    }

    private GroupItem makeGroupItem(String name) throws WmiException {
	GroupItem item = JOVALSystem.factories.sc.windows.createGroupItem();
	EntityItemStringType group = JOVALSystem.factories.sc.core.createEntityItemStringType();
	if (isBuiltinGroup(name)) {
	    group.setValue(getUser(name));
	} else {
	    group.setValue(name);
	}
	item.setGroup(group);

	StringBuffer conditions = new StringBuffer();
	conditions.append(DOMUSER_ASSOC_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(getDomain(name))));
	conditions.append(",");
	conditions.append(NAME_ASSOC_CONDITION.replaceAll("(?i)\\$username", Matcher.quoteReplacement(getUser(name))));
	String wql = GROUP_USER_WQL.replaceAll("(?i)\\$conditions", Matcher.quoteReplacement(conditions.toString()));
	for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, wql)) {
	    ISWbemPropertySet columns = row.getProperties();
	    EntityItemStringType userType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    String domain = columns.getItem("Domain").getValueAsString();
	    String user = columns.getItem("Name").getValueAsString();
	    String s = domain + "\\" + user;
	    if (isBuiltinUser(s)) {
		userType.setValue(user);
	    } else {
		userType.setValue(s);
	    }
	    item.getUser().add(userType);
	}

	return item;
    }
}
