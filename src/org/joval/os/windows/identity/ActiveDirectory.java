// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.io.LittleEndian;
import org.joval.intf.util.ILoggable;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * The ActiveDirectory class provides a mechanism to query a Windows Active Directory through the WMI provider of a machine.
 * It is case-insensitive, and it intelligently caches results so that subsequent requests for the same object can be returned
 * from memory.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class ActiveDirectory implements ILoggable {
    private static final String DOMAIN_WQL = "SELECT Name, DomainName, DnsForestName FROM Win32_NTDomain";

    private static final String AD_NAMESPACE = "root\\directory\\ldap";
    private static final String USER_WQL = "SELECT DS_userPrincipalName, DS_distinguishedName, DS_memberOf, DS_userAccountControl, DS_objectSid FROM DS_User";
    private static final String USER_WQL_UPN_CONDITION = "DS_userPrincipalName=\"$upn\"";
    private static final String GROUP_WQL = "SELECT DS_member, DS_distinguishedName, DS_objectSid FROM DS_Group";
    private static final String GROUP_WQL_CN_CONDITION = "DS_cn=\"$cn\"";
    private static final String SID_CONDITION = "DS_objectSid=\"$sid\"";

    private IWmiProvider wmi;
    private LocLogger logger;
    private Hashtable<String, User> usersByUpn;
    private Hashtable<String, User> usersBySid;
    private Hashtable<String, Group> groupsByNetbiosName;
    private Hashtable<String, Group> groupsBySid;
    private Hashtable<String, String> domains;
    private boolean initialized = false;

    ActiveDirectory(IWmiProvider wmi, LocLogger logger) {
	this.wmi = wmi;
	this.logger = logger;
	domains = new Hashtable<String, String>();
	usersByUpn = new Hashtable<String, User>();
	usersBySid = new Hashtable<String, User>();
	groupsByNetbiosName = new Hashtable<String, Group>();
	groupsBySid = new Hashtable<String, Group>();
    }

    User queryUserBySid(String sid) throws NoSuchElementException, WmiException {
	User user = usersBySid.get(sid);
	if (user == null) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(SID_CONDITION.replaceAll("(?i)\\$sid", Matcher.quoteReplacement(sid)));
	    ISWbemObjectSet os = wmi.execQuery(AD_NAMESPACE, wql.toString());
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(sid);
	    } else {
		ISWbemPropertySet props = os.iterator().next().getProperties();
		String upn = props.getItem("DS_userPrincipalName").getValueAsString();
		String dn = props.getItem("DS_distinguishedName").getValueAsString();
		String netbiosName = toNetbiosName(dn);
		String domain = getDomain(netbiosName);
		String name = Directory.getName(netbiosName);
		Collection<String> groupNetbiosNames = parseGroups(props.getItem("DS_memberOf").getValueAsArray());
		int uac = props.getItem("DS_userAccountControl").getValueAsInteger().intValue();
		boolean enabled = 0x00000002 != (uac & 0x00000002); //0x02 flag indicates disabled
		user = new User(domain, name, sid, groupNetbiosNames, enabled);
		usersByUpn.put(upn.toUpperCase(), user);
		usersBySid.put(sid, user);
	    }
	}
	return user;
    }

    User queryUser(String netbiosName) throws NoSuchElementException, IllegalArgumentException, WmiException {
	String upn = toUserPrincipalName(netbiosName);
	User user = usersByUpn.get(upn.toUpperCase());
	if (user == null) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(USER_WQL_UPN_CONDITION.replaceAll("(?i)\\$upn", Matcher.quoteReplacement(upn)));
	    ISWbemObjectSet os = wmi.execQuery(AD_NAMESPACE, wql.toString());
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(netbiosName);
	    } else {
		ISWbemPropertySet props = os.iterator().next().getProperties();
		String name = Directory.getName(netbiosName);
		String domain = getDomain(netbiosName);
		String sid = Principal.toSid(props.getItem("DS_objectSid").getValueAsString());
		Collection<String> groupNetbiosNames = parseGroups(props.getItem("DS_memberOf").getValueAsArray());
		int uac = props.getItem("DS_userAccountControl").getValueAsInteger().intValue();
		boolean enabled = 0x00000002 != (uac & 0x00000002); //0x02 flag indicates disabled
		user = new User(domain, name, sid, groupNetbiosNames, enabled);
		usersByUpn.put(upn.toUpperCase(), user);
		usersBySid.put(sid, user);
	    }
	}
	return user;
    }

    Group queryGroupBySid(String sid) throws NoSuchElementException, WmiException {
	Group group = groupsBySid.get(sid);
	if (group == null) {
	    StringBuffer wql = new StringBuffer(GROUP_WQL);
	    wql.append(" WHERE ");
	    wql.append(SID_CONDITION.replaceAll("(?i)\\$sid", Matcher.quoteReplacement(sid)));
	    ISWbemObjectSet rows = wmi.execQuery(AD_NAMESPACE, wql.toString());
	    if (rows.getSize() == 0) {
		throw new NoSuchElementException(sid);
	    } else {
		ISWbemPropertySet columns = rows.iterator().next().getProperties();
		String cn = columns.getItem("DS_cn").getValueAsString();
		String dn = columns.getItem("DS_distinguishedName").getValueAsString();
		String netbiosName = toNetbiosName(dn);
		String domain = getDomain(netbiosName);
		Collection<String> userNetbiosNames = new Vector<String>();
		Collection<String> groupNetbiosNames = new Vector<String>();
		String[] members = columns.getItem("DS_member").getValueAsArray();
		for (int i=0; i < members.length; i++) {
		    if (members[i].indexOf(",OU=Distribution Groups") != -1) {
			groupNetbiosNames.add(toNetbiosName(members[i]));
		    } else if (members[i].indexOf(",OU=Domain Users") != -1) {
			userNetbiosNames.add(toNetbiosName(members[i]));
		    } else {
			logger.warn(JOVALMsg.ERROR_AD_BAD_OU, members[i]);
		    }
		}
		group = new Group(domain, cn, sid, userNetbiosNames, groupNetbiosNames);
		groupsByNetbiosName.put(netbiosName.toUpperCase(), group);
		groupsBySid.put(sid, group);
	    }
	}
	return group;
    }

    Group queryGroup(String netbiosName) throws NoSuchElementException, IllegalArgumentException, WmiException {
	Group group = groupsByNetbiosName.get(netbiosName.toUpperCase());
	if (group == null) {
	    if (isMember(netbiosName)) {
		String domain = getDomain(netbiosName);
		String dc = toDCString(domains.get(domain.toUpperCase()));
		String cn = Directory.getName(netbiosName);

		StringBuffer wql = new StringBuffer(GROUP_WQL);
		wql.append(" WHERE ");
		wql.append(GROUP_WQL_CN_CONDITION.replaceAll("(?i)\\$cn", Matcher.quoteReplacement(cn)));
		for (ISWbemObject row : wmi.execQuery(AD_NAMESPACE, wql.toString())) {
		    ISWbemPropertySet columns = row.getProperties();
		    String dn = columns.getItem("DS_distinguishedName").getValueAsString();
		    if (dn.endsWith(dc)) {
			Collection<String> userNetbiosNames = new Vector<String>();
			Collection<String> groupNetbiosNames = new Vector<String>();
			String[] members = columns.getItem("DS_member").getValueAsArray();
			for (int i=0; i < members.length; i++) {
			    if (members[i].indexOf(",OU=Distribution Groups") != -1) {
				groupNetbiosNames.add(toNetbiosName(members[i]));
			    } else if (members[i].indexOf(",OU=Domain Users") != -1) {
				userNetbiosNames.add(toNetbiosName(members[i]));
			    } else {
				logger.warn(JOVALMsg.ERROR_AD_BAD_OU, members[i]);
			    }
			}
			String sid = Principal.toSid(columns.getItem("DS_objectSid").getValueAsString());
			group = new Group(domain, cn, sid, userNetbiosNames, groupNetbiosNames);
			groupsByNetbiosName.put(netbiosName.toUpperCase(), group);
			groupsBySid.put(sid, group);
		    } else {
			logger.trace(JOVALMsg.STATUS_AD_GROUP_SKIP, dn, dc);
		    }
		}
		if (group == null) {
		    throw new NoSuchElementException(netbiosName);
		}
	    } else {
		throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_AD_DOMAIN_UNKNOWN, netbiosName));
	    }
	}
	return group;
    }

    Principal queryPrincipal(String netbiosName) throws NoSuchElementException, WmiException {
	try {
	    return queryUser(netbiosName);
	} catch (NoSuchElementException e) {
	}
	return queryGroup(netbiosName);
    }

    Principal queryPrincipalBySid(String sid) throws NoSuchElementException, WmiException {
	try {
	    return queryUserBySid(sid);
	} catch (NoSuchElementException e) {
	}
	return queryGroupBySid(sid);
    }

    boolean isMember(String netbiosName) throws IllegalArgumentException {
	init();
	return domains.containsKey(getDomain(netbiosName));
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

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    /**
     * Initialize the domain list.
     */
    private void init() {
	if (initialized) {
	    return;
	}
	try {
	    for (ISWbemObject row : wmi.execQuery(IWmiProvider.CIMv2, DOMAIN_WQL)) {
		ISWbemPropertySet columns = row.getProperties();
		String domain = columns.getItem("DomainName").getValueAsString();
		String dns = columns.getItem("DnsForestName").getValueAsString();
		String name = columns.getItem("Name").getValueAsString();
		if (domain == null || dns == null) {
		    logger.trace(JOVALMsg.STATUS_AD_DOMAIN_SKIP, name);
		} else {
		    logger.trace(JOVALMsg.STATUS_AD_DOMAIN_ADD, domain, dns);
		    domains.put(domain.toUpperCase(), dns);
		}
	    }
	    initialized = true;
	} catch (WmiException e) {
	    logger.warn(JOVALMsg.ERROR_AD_INIT);
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Given a name of the form DOMAIN\\username, return the corresponding UserPrincipalName of the form username@domain.com.
     */
    private String toUserPrincipalName(String netbiosName) throws IllegalArgumentException {
	String domain = getDomain(netbiosName);
	if (isMember(netbiosName)) {
	    String dns = domains.get(domain.toUpperCase());
	    String upn = new StringBuffer(Directory.getName(netbiosName)).append("@").append(dns).toString();
	    logger.trace(JOVALMsg.STATUS_UPN_CONVERT, netbiosName, upn);
	    return upn;
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_AD_DOMAIN_UNKNOWN, netbiosName));
	}
    }

    /**
     * Convert a DNS path into a Netbios domain name.
     */
    private String toDomain(String dns) {
	for (String domain : domains.keySet()) {
	    if (dns.equals(domains.get(domain))) {
		return domain;
	    }
	}
	return null;
    }

    /**
     * Convert a String of the form a.b.com to a String of the form DC=a,DC=b,DC=com.
     */
    private String toDCString(String dns) {
	StringBuffer sb = new StringBuffer();
	for (String token : StringTools.toList(StringTools.tokenize(dns, "."))) {
	    if (sb.length() > 0) {
		sb.append(",");
	    }
	    sb.append("DC=");
	    sb.append(token);
	}
	return sb.toString();
    }

    /**
     * Get the Domain portion of a Domain\\Name String.
     */
    private String getDomain(String s) throws IllegalArgumentException {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_AD_DOMAIN_REQUIRED, s));
	} else {
	    return s.substring(0, ptr);
	}
    }

    /**
     * Convert a DN to a Netbios Name.
     *
     * @throws NoSuchElementException if the domain can not be found
     */
    private String toNetbiosName(String dn) throws NoSuchElementException {
	int ptr = dn.indexOf(",");
	String groupName = dn.substring(3, ptr); // Starts with CN=
	ptr = dn.indexOf(",DC=");
	StringBuffer dns = new StringBuffer();
	for (String name : StringTools.toList(StringTools.tokenize(dn.substring(ptr), ",DC="))) {
	    if (dns.length() > 0) {
		dns.append(".");
	    }
	    dns.append(name);
	}
	String domain = toDomain(dns.toString());
	if (domain == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.STATUS_NAME_DOMAIN_ERR, dn));
	}
	String name = domain + "\\" + groupName;
	logger.trace(JOVALMsg.STATUS_NAME_DOMAIN_OK, dn, name);
	return name;
    }

    /**
     * Convert a String of group DNs into a Collection of DOMAIN\\group names.
     */
    private Collection<String> parseGroups(String[] sa) {
	Collection<String> groups = new Vector<String>(sa.length);
	for (int i=0; i < sa.length; i++) {
	    try {
		groups.add(toNetbiosName(sa[i]));
	    } catch (NoSuchElementException e) {
		logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return groups;
    }
}
