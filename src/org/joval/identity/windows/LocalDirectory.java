// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity.windows;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;

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
    static final String USER_WQL		= "SELECT SID, Name, Domain, Disabled FROM Win32_UserAccount";
    static final String GROUP_WQL		= "SELECT SID, Name, Domain FROM Win32_Group";
    static final String DOMAIN_CONDITION	= "Domain='$domain'";
    static final String NAME_CONDITION		= "Name='$name'";

    static final String USER_GROUP_WQL		= "ASSOCIATORS OF {$conditions} WHERE resultClass=Win32_Group";
    static final String USER_DOMAIN_CONDITION	= "Win32_UserAccount.Domain=\"$domain\"";
    static final String USER_NAME_CONDITION	= "Name=\"$username\"";

    static final String GROUP_USER_WQL		= "SELECT * FROM Win32_GroupUser WHERE GroupComponent=\"$conditions\"";
    static final String GROUP_DOMAIN_CONDITION	= "Win32_Group.Domain='$domain'";

    private Hashtable<String, User> users;
    private Hashtable<String, Group> groups;

    private String hostname;
    private IWmiProvider wmi;
    private boolean preloadedUsers = false;
    private boolean preloadedGroups = false;

    public LocalDirectory(String hostname, IWmiProvider wmi) {
	this.hostname = hostname;
	this.wmi = wmi;
	users = new Hashtable<String, User>();
	groups = new Hashtable<String, Group>();
    }

    /**
     * Query for an individual user.  The input parameter should be of the form DOMAIN\\username.  For built-in accounts,
     * the DOMAIN\\ part can be dropped, in which case the input parameter can be just the username.
     *
     * @throws NoSuchElementException if the user does not exist
     */
    public User queryUser(String netbiosName) throws NoSuchElementException, WmiException {
	String domain = getDomain(netbiosName);
	String name = getName(netbiosName);
	netbiosName = domain + "\\" + name; // in case no domain was specified in the original netbiosName

	User user = users.get(netbiosName.toUpperCase());
	if (user == null) {
	    if (preloadedUsers) {
		throw new NoSuchElementException(netbiosName);
	    }

	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(NAME_CONDITION.replaceAll("(?i)\\$name", Matcher.quoteReplacement(name)));
	    wql.append(" AND ");
	    wql.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));

	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql.toString());
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(netbiosName);
	    } else {
		ISWbemPropertySet columns = os.iterator().next().getProperties();
		boolean enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
		String sid = columns.getItem("SID").getValueAsString();
		user = makeUser(domain, name, sid, enabled);
		users.put(netbiosName.toUpperCase(), user);
	    }
	}
	return user;
    }

    /**
     * Returns a List of all the local users.
     */
    public Collection<User> queryAllUsers() throws WmiException {
	if (!preloadedUsers) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(hostname)));
	    for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, wql.toString())) {
		ISWbemPropertySet columns = row.getProperties();
		String domain = columns.getItem("Domain").getValueAsString();
		String name = columns.getItem("Name").getValueAsString();
		String netbiosName = domain + "\\" + name;
		String sid = columns.getItem("SID").getValueAsString();
		boolean enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
		if (users.get(netbiosName.toUpperCase()) == null) {
		    users.put(netbiosName.toUpperCase(), makeUser(domain, name, sid, enabled));
		}
	    }
	    preloadedUsers = true;
	}
	return users.values();
    }

    /**
     * Query for an individual group.  The input parameter should be of the form DOMAIN\\name.  For built-in groups, the
     * DOMAIN\\ part can be dropped, in which case the name parameter is just the group name.
     *
     * @throws NoSuchElementException if the group does not exist
     */
    public Group queryGroup(String netbiosName) throws NoSuchElementException, WmiException {
	String domain = getDomain(netbiosName);
	String name = getName(netbiosName);
	netbiosName = domain + "\\" + name; // in case no domain was specified in the original netbiosName

	Group group = groups.get(netbiosName.toUpperCase());
	if (group == null) {
	    if (preloadedGroups) {
		throw new NoSuchElementException(netbiosName);
	    }

	    StringBuffer wql = new StringBuffer(GROUP_WQL);
	    wql.append(" WHERE ");
	    wql.append(NAME_CONDITION.replaceAll("(?i)\\$name", Matcher.quoteReplacement(name)));
	    wql.append(" AND ");
	    wql.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));

	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql.toString());
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(netbiosName);
	    } else {
		ISWbemPropertySet columns = os.iterator().next().getProperties();
		String sid = columns.getItem("SID").getValueAsString();
		group = makeGroup(domain, name, sid);
	    }
	    groups.put(netbiosName.toUpperCase(), group);
	}
	return group;
    }

    /**
     * Returns a List of all the local groups.
     */
    public Collection<Group> queryAllGroups() throws WmiException {
	if (!preloadedGroups) {
	    StringBuffer wql = new StringBuffer(GROUP_WQL);
	    wql.append(" WHERE ");
	    wql.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(hostname)));
	    for (ISWbemObject rows : wmi.execQuery(IWmiProvider.CIMv2, wql.toString())) {
		ISWbemPropertySet columns = rows.getProperties();
		String domain = columns.getItem("Domain").getValueAsString();
		String name = columns.getItem("Name").getValueAsString();
		String netbiosName = domain + "\\" + name;
		String sid = columns.getItem("SID").getValueAsString();
		if (groups.get(netbiosName.toUpperCase()) == null) {
		    groups.put(netbiosName.toUpperCase(), makeGroup(domain, name, sid));
		}
	    }
	    preloadedGroups = true;
	}
	return groups.values();
    }

    /**
     * Returns true of the supplied NetBios username String represents a built-in account on the local machine.
     */
    public boolean isBuiltinUser(String netbiosName) {
	if (isMember(netbiosName)) {
	    String username = getName(netbiosName);
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

    /**
     * Returns true of the supplied NetBios group name String represents a built-in group on the local machine.
     */
    public boolean isBuiltinGroup(String netbiosName) {
	if (isMember(netbiosName)) {
	    String group = getName(netbiosName);
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

    /**
     * Get the Name portion of a Domain\\Name String.  If there is no domain portion, returns the original String.
     */
    public String getName(String s) {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    return s;
	} else {
	    return s.substring(ptr+1);
	}
    }

    /**
     * Returns whether or not the specified netbiosName is a member of this directory, meaning that the domain matches
     * the local hostname.
     */
    public boolean isMember(String netbiosName) {
	return hostname.equalsIgnoreCase(getDomain(netbiosName));
    }

    // Private

    /**
     * Get the Domain portion of a Domain\\Name String.  If Domain is not specified, this method returns the hostname
     * used in the LocalDirectory constructor.
     */
    private String getDomain(String s) {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    return hostname;
	} else {
	    return s.substring(0, ptr);
	}
    }

    private User makeUser(String domain, String name, String sid, boolean enabled) throws WmiException {
	StringBuffer conditions = new StringBuffer();
	conditions.append(USER_DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));
	conditions.append(",");
	conditions.append(USER_NAME_CONDITION.replaceAll("(?i)\\$username", Matcher.quoteReplacement(name)));
	String wql = USER_GROUP_WQL.replaceAll("(?i)\\$conditions", Matcher.quoteReplacement(conditions.toString()));

	ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql);
	List<String> groupNetbiosNames = new Vector<String>();
	for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, wql)) {
	    ISWbemPropertySet columns = row.getProperties();
	    String groupDomain = columns.getItem("Domain").getValueAsString();
	    String groupName = columns.getItem("Name").getValueAsString();
	    groupNetbiosNames.add(groupDomain + "\\" + groupName);
	}
	return new User(domain, name, sid, groupNetbiosNames, enabled);
    }

    private Group makeGroup(String domain, String name, String sid) throws WmiException {
	StringBuffer conditions = new StringBuffer();
	conditions.append(GROUP_DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));
	conditions.append(",");
	conditions.append(NAME_CONDITION.replaceAll("(?i)\\$name", Matcher.quoteReplacement(name)));
	String wql = GROUP_USER_WQL.replaceAll("(?i)\\$conditions", Matcher.quoteReplacement(conditions.toString()));

	ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql);
	List<String> groupNetbiosNames = new Vector<String>(), userNetbiosNames = new Vector<String>();
	for (ISWbemObject row : os) {
	    ISWbemPropertySet columns = row.getProperties();

	    String partComponent = columns.getItem("PartComponent").getValueAsString();
	    String memberData = partComponent.substring(partComponent.indexOf(":") + 1);
	    int ptr = memberData.indexOf(",");
	    String clazz = memberData.substring(0, memberData.indexOf(".Domain="));
	    int begin = memberData.indexOf("Domain=\"") + 8;
	    int end = memberData.indexOf("\"", begin+1);
	    String memberDomain = memberData.substring(begin, end);
	    begin = memberData.indexOf("Name=\"") + 6;
	    end = memberData.indexOf("\"", begin+1);
	    String memberName = memberData.substring(begin, end);

	    if ("Win32_UserAccount".equals(clazz)) {
		userNetbiosNames.add(memberDomain + "\\" + memberName);
	    } else if ("Win32_Group".equals(clazz)) {
		groupNetbiosNames.add(memberDomain + "\\" + memberName);
	    }
	}
	return new Group(domain, name, sid, userNetbiosNames, groupNetbiosNames);
    }
}
