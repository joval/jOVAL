// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity.windows;

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
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.windows.wmi.WmiException;

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
    private static final String USER_WQL = "SELECT DS_memberOf, DS_userAccountControl, DS_objectSid FROM DS_User";
    private static final String USER_WQL_UPN_CONDITION = "DS_userPrincipalName=\"$upn\"";

    private IWmiProvider wmi;
    private Hashtable<String, User> users;
    private Hashtable<String, String> domains;

    public ActiveDirectory(IWmiProvider wmi) {
	this.wmi = wmi;
	domains = new Hashtable<String, String>();
	users = new Hashtable<String, User>();
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
	} catch (WmiException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WMI_GENERAL", e.getMessage()), e);
	}
    }

    public User queryUser(String netbiosName) throws NoSuchElementException, IllegalArgumentException, WmiException {
	String upn = toUserPrincipalName(netbiosName);
	User user = users.get(upn.toUpperCase());
	if (user == null) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(USER_WQL_UPN_CONDITION.replaceAll("(?i)\\$upn", Matcher.quoteReplacement(upn)));
	    ISWbemObjectSet os = wmi.execQuery(AD_NAMESPACE, wql.toString());
	    if (os.getSize() == 0) {
		throw new NoSuchElementException(netbiosName);
	    } else {
		user = makeUser(netbiosName, os.iterator().next().getProperties());
	    }
	    users.put(upn.toUpperCase(), user);
	}
	return user;
    }

    // Private

    /**
     * Given a name of the form DOMAIN\\username, return the corresponding UserPrincipalName of the form username@domain.com.
     */
    private String toUserPrincipalName(String netbiosName) throws IllegalArgumentException {
	String domain = getDomain(netbiosName);
	String dns = domains.get(domain.toUpperCase());
	if (dns == null) {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_AD_DOMAIN_UNKNOWN", domain));
	} else {
	    String upn = new StringBuffer(getName(netbiosName)).append("@").append(dns).toString();
	    JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_UPN_CONVERT", netbiosName, upn));
	    return upn;
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
     * Convert a String of group DNs into a List of DOMAIN\\group names.
     */
    private List<String> parseGroups(String[] sa) {
	List<String> groups = new Vector<String>(sa.length);
	for (int i=0; i < sa.length; i++) {
	    String dn = sa[i];
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
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_GROUP_DOMAIN_ERR", dn));
	    } else {
		String name = domain + "\\" + groupName;
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_GROUP_DOMAIN_OK", dn, name));
		groups.add(name);
	    }
	}
	return groups;
    }

    private User makeUser(String netbiosName, ISWbemPropertySet props) throws WmiException {
	String name = getName(netbiosName);
	String domain = getDomain(netbiosName);
	String sid = toSid(props.getItem("DS_objectSid").getValueAsString());
	List<String> groupNetbiosNames = parseGroups(props.getItem("DS_memberOf").getValueAsArray());
	int uac = props.getItem("DS_userAccountControl").getValueAsInteger().intValue();
	boolean enabled = 0x00000002 != (uac & 0x00000002); //0x02 flag indicates disabled
	return new User(domain, name, sid, groupNetbiosNames, enabled);
    }

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
