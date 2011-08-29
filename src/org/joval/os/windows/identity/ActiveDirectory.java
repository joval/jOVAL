// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joval.io.LittleEndian;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;
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
public class ActiveDirectory {
    private static final String DOMAIN_WQL = "SELECT Name, DomainName, DnsForestName FROM Win32_NTDomain";

    private static final String AD_NAMESPACE = "root\\directory\\ldap";
    private static final String USER_WQL = "SELECT DS_userPrincipalName, DS_distinguishedName, DS_memberOf, DS_userAccountControl, DS_objectSid FROM DS_User";
    private static final String USER_WQL_UPN_CONDITION = "DS_userPrincipalName=\"$upn\"";
    private static final String GROUP_WQL = "SELECT DS_member, DS_distinguishedName, DS_objectSid FROM DS_Group";
    private static final String GROUP_WQL_CN_CONDITION = "DS_cn=\"$cn\"";
    private static final String SID_CONDITION = "DS_objectSid=\"$sid\"";

    private IWmiProvider wmi;
    private Hashtable<String, User> usersByUpn;
    private Hashtable<String, User> usersBySid;
    private Hashtable<String, Group> groupsByNetbiosName;
    private Hashtable<String, Group> groupsBySid;
    private Hashtable<String, String> domains;
    private boolean initialized = false;

    public ActiveDirectory(IWmiProvider wmi) {
	this.wmi = wmi;
	domains = new Hashtable<String, String>();
	usersByUpn = new Hashtable<String, User>();
	usersBySid = new Hashtable<String, User>();
	groupsByNetbiosName = new Hashtable<String, Group>();
	groupsBySid = new Hashtable<String, Group>();
    }

    public User queryUserBySid(String sid) throws NoSuchElementException, WmiException {
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
		String name = getName(netbiosName);
		List<String> groupNetbiosNames = parseGroups(props.getItem("DS_memberOf").getValueAsArray());
		int uac = props.getItem("DS_userAccountControl").getValueAsInteger().intValue();
		boolean enabled = 0x00000002 != (uac & 0x00000002); //0x02 flag indicates disabled
		user = new User(domain, name, sid, groupNetbiosNames, enabled);
		usersByUpn.put(upn.toUpperCase(), user);
		usersBySid.put(sid, user);
	    }
	}
	return user;
    }

    public User queryUser(String netbiosName) throws NoSuchElementException, IllegalArgumentException, WmiException {
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
		String name = getName(netbiosName);
		String domain = getDomain(netbiosName);
		String sid = toSid(props.getItem("DS_objectSid").getValueAsString());
		List<String> groupNetbiosNames = parseGroups(props.getItem("DS_memberOf").getValueAsArray());
		int uac = props.getItem("DS_userAccountControl").getValueAsInteger().intValue();
		boolean enabled = 0x00000002 != (uac & 0x00000002); //0x02 flag indicates disabled
		user = new User(domain, name, sid, groupNetbiosNames, enabled);
		usersByUpn.put(upn.toUpperCase(), user);
		usersBySid.put(sid, user);
	    }
	}
	return user;
    }

    public Group queryGroupBySid(String sid) throws NoSuchElementException, WmiException {
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
		List<String> userNetbiosNames = new Vector<String>();
		List<String> groupNetbiosNames = new Vector<String>();
		String[] members = columns.getItem("DS_member").getValueAsArray();
		for (int i=0; i < members.length; i++) {
		    if (members[i].indexOf(",OU=Distribution Groups") != -1) {
			groupNetbiosNames.add(toNetbiosName(members[i]));
		    } else if (members[i].indexOf(",OU=Domain Users") != -1) {
			userNetbiosNames.add(toNetbiosName(members[i]));
		    } else {
			JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_AD_BAD_OU", members[i]));
		    }
		}
		group = new Group(domain, cn, sid, userNetbiosNames, groupNetbiosNames);
		groupsByNetbiosName.put(netbiosName.toUpperCase(), group);
		groupsBySid.put(sid, group);
	    }
	}
	return group;
    }

    public Group queryGroup(String netbiosName) throws NoSuchElementException, IllegalArgumentException, WmiException {
	Group group = groupsByNetbiosName.get(netbiosName.toUpperCase());
	if (group == null) {
	    if (isMember(netbiosName)) {
		String domain = getDomain(netbiosName);
		String dc = toDCString(domains.get(domain.toUpperCase()));
		String cn = getName(netbiosName);

		StringBuffer wql = new StringBuffer(GROUP_WQL);
		wql.append(" WHERE ");
		wql.append(GROUP_WQL_CN_CONDITION.replaceAll("(?i)\\$cn", Matcher.quoteReplacement(cn)));
		for (ISWbemObject row : wmi.execQuery(AD_NAMESPACE, wql.toString())) {
		    ISWbemPropertySet columns = row.getProperties();
		    String dn = columns.getItem("DS_distinguishedName").getValueAsString();
		    if (dn.endsWith(dc)) {
			List<String> userNetbiosNames = new Vector<String>();
			List<String> groupNetbiosNames = new Vector<String>();
			String[] members = columns.getItem("DS_member").getValueAsArray();
			for (int i=0; i < members.length; i++) {
			    if (members[i].indexOf(",OU=Distribution Groups") != -1) {
				groupNetbiosNames.add(toNetbiosName(members[i]));
			    } else if (members[i].indexOf(",OU=Domain Users") != -1) {
				userNetbiosNames.add(toNetbiosName(members[i]));
			    } else {
				JOVALSystem.getLogger().log(Level.WARNING,
							    JOVALSystem.getMessage("ERROR_AD_BAD_OU", members[i]));
			    }
			}
			String sid = toSid(columns.getItem("DS_objectSid").getValueAsString());
			group = new Group(domain, cn, sid, userNetbiosNames, groupNetbiosNames);
			groupsByNetbiosName.put(netbiosName.toUpperCase(), group);
			groupsBySid.put(sid, group);
		    } else {
			JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_AD_GROUP_SKIP", dn, dc));
		    }
		}
		if (group == null) {
		    throw new NoSuchElementException(netbiosName);
		}
	    } else {
		throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_AD_DOMAIN_UNKNOWN", netbiosName));
	    }
	}
	return group;
    }

    public boolean isMember(String netbiosName) throws IllegalArgumentException {
	init();
	return domains.containsKey(getDomain(netbiosName));
    }

    public boolean isMemberSid(String sid) {
	return false;
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
		    JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_AD_DOMAIN_SKIP", name));
		} else {
		    JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_AD_DOMAIN_ADD", domain, dns));
		    domains.put(domain.toUpperCase(), dns);
		}
	    }
	    initialized = true;
	} catch (WmiException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WMI_GENERAL", e.getMessage()), e);
	}
    }

    /**
     * Given a name of the form DOMAIN\\username, return the corresponding UserPrincipalName of the form username@domain.com.
     */
    private String toUserPrincipalName(String netbiosName) throws IllegalArgumentException {
	String domain = getDomain(netbiosName);
	if (isMember(netbiosName)) {
	    String dns = domains.get(domain.toUpperCase());
	    String upn = new StringBuffer(getName(netbiosName)).append("@").append(dns).toString();
	    JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_UPN_CONVERT", netbiosName, upn));
	    return upn;
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_AD_DOMAIN_UNKNOWN", netbiosName));
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
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_AD_DOMAIN_REQUIRED", s));
	} else {
	    return s.substring(0, ptr);
	}
    }

    /**
     * Get the Name portion of a Domain\\Name String.  If there is no domain portion, throws an exception.
     */
    private String getName(String s) throws IllegalArgumentException {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_AD_DOMAIN_REQUIRED", s));
	} else {
	    return s.substring(ptr+1);
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
	    throw new NoSuchElementException(JOVALSystem.getMessage("STATUS_NAME_DOMAIN_ERR", dn));
	}
	String name = domain + "\\" + groupName;
	JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_NAME_DOMAIN_OK", dn, name));
	return name;
    }

    /**
     * Convert a String of group DNs into a List of DOMAIN\\group names.
     */
    private List<String> parseGroups(String[] sa) {
	List<String> groups = new Vector<String>(sa.length);
	for (int i=0; i < sa.length; i++) {
	    try {
		groups.add(toNetbiosName(sa[i]));
	    } catch (NoSuchElementException e) {
		JOVALSystem.getLogger().log(Level.FINER, e.getMessage());
	    }
	}
	return groups;
    }

    /**
     * Convert a hexidecimal String representation of a SID into a "readable" SID String.
     */
    private String toSid(String hex) {
	int rev = Integer.parseInt(hex.substring(0, 2), 16);
	int subauthCount = Integer.parseInt(hex.substring(2, 4), 16);

	String idAuthStr = hex.substring(4, 16);
	long idAuth = Long.parseLong(idAuthStr, 16);

	StringBuffer sid = new StringBuffer("S-");
	sid.append(Integer.toHexString(rev));
	sid.append("-");
	sid.append(Long.toHexString(idAuth));

	for (int i=0; i < subauthCount; i++) {
	    sid.append("-");
	    byte[] buff = new byte[4];
	    int base = 16 + i*8;
	    buff[0] = (byte)Integer.parseInt(hex.substring(base, base+2), 16);
	    buff[1] = (byte)Integer.parseInt(hex.substring(base+2, base+4), 16);
	    buff[2] = (byte)Integer.parseInt(hex.substring(base+4, base+6), 16);
	    buff[3] = (byte)Integer.parseInt(hex.substring(base+6, base+8), 16);
	    sid.append(Long.toString(LittleEndian.getUInt(buff) & 0xFFFFFFFFL));
	}
	return sid.toString();
    }
}
