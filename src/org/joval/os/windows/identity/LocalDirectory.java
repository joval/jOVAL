// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.util.ILoggable;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * The LocalDirectory class provides a mechanism to query the local User/Group directory for a Windows machine.  It is
 * case-insensitive, and it intelligently caches results so that subsequent requests for the same object can be returned
 * from memory.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class LocalDirectory implements ILoggable {
    static final String USER_WQL		= "SELECT SID, Name, Domain, Disabled FROM Win32_UserAccount";
    static final String SYSUSER_WQL		= "SELECT SID, Name, Domain FROM Win32_SystemAccount";
    static final String GROUP_WQL		= "SELECT SID, Name, Domain FROM Win32_Group";
    static final String DOMAIN_CONDITION	= "Domain='$domain'";
    static final String NAME_CONDITION		= "Name='$name'";
    static final String SID_CONDITION		= "SID='$sid'";

    static final String USER_GROUP_WQL		= "ASSOCIATORS OF {$conditions} WHERE resultClass=Win32_Group";
    static final String USER_DOMAIN_CONDITION	= "Win32_UserAccount.Domain=\"$domain\"";
    static final String USER_NAME_CONDITION	= "Name=\"$username\"";

    static final String GROUP_USER_WQL		= "SELECT * FROM Win32_GroupUser WHERE GroupComponent=\"$conditions\"";
    static final String GROUP_DOMAIN_CONDITION	= "Win32_Group.Domain='$domain'";

    private Hashtable<String, IUser> usersBySid;
    private Hashtable<String, IUser> usersByNetbiosName;
    private Hashtable<String, IGroup> groupsBySid;
    private Hashtable<String, IGroup> groupsByNetbiosName;
    private Collection<String> builtinUsers;
    private Collection<String> builtinGroups;

    private String hostname;
    private IWmiProvider wmi;
    private LocLogger logger;
    private boolean preloadedUsers = false;
    private boolean preloadedGroups = false;

    LocalDirectory(String hostname, LocLogger logger) {
	this.hostname = hostname;
	this.logger = logger;
	usersByNetbiosName = new Hashtable<String, IUser>();
	usersBySid = new Hashtable<String, IUser>();
	groupsByNetbiosName = new Hashtable<String, IGroup>();
	groupsBySid = new Hashtable<String, IGroup>();

	builtinUsers = new HashSet<String>();
	builtinUsers.add("Administrator".toUpperCase());
	builtinUsers.add("Guest".toUpperCase());
	builtinUsers.add("HomeGroupUser$".toUpperCase());

	builtinGroups = new HashSet<String>();
	builtinGroups.add("Account Operators".toUpperCase());
	builtinGroups.add("Administrators".toUpperCase());
	builtinGroups.add("Backup Operators".toUpperCase());
	builtinGroups.add("Cryptographic Operators".toUpperCase());
	builtinGroups.add("Distributed COM Users".toUpperCase());
	builtinGroups.add("Event Log Readers".toUpperCase());
	builtinGroups.add("Guests".toUpperCase());
	builtinGroups.add("HelpLibraryUpdaters".toUpperCase());
	builtinGroups.add("HomeUsers".toUpperCase());
	builtinGroups.add("IIS_IUSRS".toUpperCase());
	builtinGroups.add("Network Configuration Operators".toUpperCase());
	builtinGroups.add("Performance Log Users".toUpperCase());
	builtinGroups.add("Performance Monitor Users".toUpperCase());
	builtinGroups.add("Power Users".toUpperCase());
	builtinGroups.add("Print Operators".toUpperCase());
	builtinGroups.add("Remote Desktop Users".toUpperCase());
	builtinGroups.add("Replicator".toUpperCase());
	builtinGroups.add("Server Operators".toUpperCase());
	builtinGroups.add("Users".toUpperCase());
    }

    void setWmiProvider(IWmiProvider wmi) {
	this.wmi = wmi;
    }

    IUser queryUserBySid(String sid) throws NoSuchElementException, WmiException {
	IUser user = usersBySid.get(sid);
	if (user == null) {
	    if (preloadedUsers) {
		throw new NoSuchElementException(sid);
	    }

	    boolean system = false;
	    StringBuffer conditions = new StringBuffer(" WHERE ");
	    conditions.append(SID_CONDITION.replaceAll("(?i)\\$sid", Matcher.quoteReplacement(sid)));
	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, USER_WQL + conditions.toString());
	    if (os.getSize() == 0) {
		system = true;
		os = wmi.execQuery(IWmiProvider.CIMv2, SYSUSER_WQL + conditions.toString());
	    }
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(sid);
	    } else {
		user = preloadUser(os.iterator().next().getProperties(), system);
	    }
	}
	return user;
    }

    /**
     * Query for an individual user.  The input parameter should be of the form DOMAIN\\username.  For built-in accounts,
     * the DOMAIN\\ part can be dropped, in which case the input parameter can be just the username.
     *
     * @throws NoSuchElementException if the user does not exist
     */
    IUser queryUser(String netbiosName) throws NoSuchElementException, WmiException {
	String domain = getDomain(netbiosName);
	String name = Directory.getName(netbiosName);
	netbiosName = domain + "\\" + name; // in case no domain was specified in the original netbiosName

	IUser user = usersByNetbiosName.get(netbiosName.toUpperCase());
	if (user == null) {
	    if (preloadedUsers) {
		throw new NoSuchElementException(netbiosName);
	    }

	    boolean system = false;
	    StringBuffer conditions = new StringBuffer(" WHERE ");
	    conditions.append(NAME_CONDITION.replaceAll("(?i)\\$name", Matcher.quoteReplacement(name)));
	    conditions.append(" AND ");
	    conditions.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(domain)));
	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, USER_WQL + conditions.toString());
	    if (os.getSize() == 0) {
		system = true;
		os = wmi.execQuery(IWmiProvider.CIMv2, SYSUSER_WQL + conditions.toString());
	    }
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(netbiosName);
	    } else {
		user = preloadUser(os.iterator().next().getProperties(), system);
	    }
	}
	return user;
    }

    /**
     * Returns a Collection of all the local users.
     */
    Collection<IUser> queryAllUsers() throws WmiException {
	if (!preloadedUsers) {
	    StringBuffer conditions = new StringBuffer(" WHERE ");
	    conditions.append(DOMAIN_CONDITION.replaceAll("(?i)\\$domain", Matcher.quoteReplacement(hostname)));
	    for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, USER_WQL + conditions.toString())) {
		preloadUser(row.getProperties(), false);
	    }
	    for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, SYSUSER_WQL + conditions.toString())) {
		preloadUser(row.getProperties(), true);
	    }
	    preloadedUsers = true;
	}
	return usersByNetbiosName.values();
    }

    IGroup queryGroupBySid(String sid) throws NoSuchElementException, WmiException {
	IGroup group = groupsBySid.get(sid);
	if (group == null) {
	    if (preloadedGroups) {
		throw new NoSuchElementException(sid);
	    }

	    StringBuffer wql = new StringBuffer(GROUP_WQL);
	    wql.append(" WHERE ");
	    wql.append(SID_CONDITION.replaceAll("(?i)\\$sid", Matcher.quoteReplacement(sid)));

	    ISWbemObjectSet os = wmi.execQuery(IWmiProvider.CIMv2, wql.toString());
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(sid);
	    } else {
		ISWbemPropertySet columns = os.iterator().next().getProperties();
		String name = columns.getItem("Name").getValueAsString();
		String domain = columns.getItem("Domain").getValueAsString();
		group = makeGroup(domain, name, sid);
		groupsByNetbiosName.put((domain + "\\" + name).toUpperCase(), group);
		groupsBySid.put(sid, group);
	    }
	}
	return group;
    }

    /**
     * Query for an individual group.  The input parameter should be of the form DOMAIN\\name.  For built-in groups, the
     * DOMAIN\\ part can be dropped, in which case the name parameter is just the group name.
     *
     * @throws NoSuchElementException if the group does not exist
     */
    IGroup queryGroup(String netbiosName) throws NoSuchElementException, WmiException {
	String domain = getDomain(netbiosName);
	String name = Directory.getName(netbiosName);
	netbiosName = domain + "\\" + name; // in case no domain was specified in the original netbiosName

	IGroup group = groupsByNetbiosName.get(netbiosName.toUpperCase());
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
		groupsByNetbiosName.put(netbiosName.toUpperCase(), group);
		groupsBySid.put(sid, group);
	    }
	}
	return group;
    }

    /**
     * Returns a Collection of all the local groups.
     */
    Collection<IGroup> queryAllGroups() throws WmiException {
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
		if (groupsByNetbiosName.get(netbiosName.toUpperCase()) == null) {
		    Group group = makeGroup(domain, name, sid);
		    groupsByNetbiosName.put(netbiosName.toUpperCase(), group);
		    groupsBySid.put(sid, group);
		}
	    }
	    preloadedGroups = true;
	}
	return groupsByNetbiosName.values();
    }

    /**
     * Returns a Principal (User or Group) given a Netbios name.
     */
    IPrincipal queryPrincipal(String netbiosName) throws NoSuchElementException, WmiException {
	try {
	    return queryUser(netbiosName);
	} catch (NoSuchElementException e) {
	}
	return queryGroup(netbiosName);
    }

    /**
     * Returns a Principal (User or Group) given a sid.
     */
    IPrincipal queryPrincipalBySid(String sid) throws NoSuchElementException, WmiException {
	try {
	    return queryUserBySid(sid);
	} catch (NoSuchElementException e) {
	}
	return queryGroupBySid(sid);
    }

    /**
     * Returns a Collection of all local users and groups.
     */
    Collection<IPrincipal> queryAllPrincipals() throws WmiException {
	Collection<IPrincipal> result = new Vector<IPrincipal>();
	result.addAll(queryAllUsers());
	result.addAll(queryAllGroups());
	return result;
    }

    /**
     * Returns true of the supplied NetBios username String represents a built-in account on the local machine.
     */
    boolean isBuiltinUser(String netbiosName) {
	if (isMember(netbiosName)) {
	    return builtinUsers.contains(Directory.getName(netbiosName).toUpperCase());
	}
	return false;
    }

    /**
     * Returns true if the supplied NetBios group name String represents a built-in group on the local machine.
     */
    boolean isBuiltinGroup(String netbiosName) {
	if (isMember(netbiosName)) {
	    return builtinGroups.contains(Directory.getName(netbiosName).toUpperCase());
	}
	return false;
    }

    /**
     * Returns whether or not the specified netbiosName is a member of this directory, meaning that the domain matches
     * the local hostname.
     */
    boolean isMember(String netbiosName) {
	return hostname.equalsIgnoreCase(getDomain(netbiosName));
    }

    boolean isMemberSid(String sid) {
	try {
	    queryPrincipalBySid(sid);
	    return true;
	} catch (NoSuchElementException e) {
	} catch (WmiException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return false;
    }

    /**
     * Fills in the domain with the local hostname if it is not specified in the argument.
     */
    String getQualifiedNetbiosName(String netbiosName) {
	String domain = getDomain(netbiosName);
	if (domain == null) {
	    domain = hostname.toUpperCase();
	}
	return domain + "\\" + Directory.getName(netbiosName);
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    private IUser preloadUser(ISWbemPropertySet columns, boolean builtin) throws WmiException {
	String domain = columns.getItem("Domain").getValueAsString();
	String name = columns.getItem("Name").getValueAsString();
	if (builtin) {
	    builtinUsers.add(name.toUpperCase());
	}
	String netbiosName = domain + "\\" + name;
	String sid = columns.getItem("SID").getValueAsString();
	boolean enabled = true;
	if (columns.getItem("Disabled") != null) {
	    enabled = !columns.getItem("Disabled").getValueAsBoolean().booleanValue();
	}
	IUser user = usersByNetbiosName.get(netbiosName.toUpperCase());
	if (user == null) {
	    user = makeUser(domain, name, sid, enabled);
	    usersByNetbiosName.put(netbiosName.toUpperCase(), user);
	    usersBySid.put(sid, user);
	}
	return user;
    }

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
	Collection<String> groupNetbiosNames = new Vector<String>();
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
	Collection<String> groupNetbiosNames = new Vector<String>(), userNetbiosNames = new Vector<String>();
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
