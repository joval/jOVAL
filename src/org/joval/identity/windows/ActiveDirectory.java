// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity.windows;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.UserItem;
import oval.schemas.results.core.ResultEnumeration;

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
    private static final String USER_WQL = "SELECT DS_userPrincipalName, DS_memberOf, DS_userAccountControl FROM DS_User";
    private static final String USER_WQL_UPN_CONDITION = "DS_userPrincipalName=\"$upn\"";

    private IWmiProvider wmi;
    private Hashtable<String, UserItem> users;
    private Hashtable<String, String> domains;
    private boolean preloaded = false;

    public ActiveDirectory(IWmiProvider wmi) {
	this.wmi = wmi;
	domains = new Hashtable<String, String>();
	users = new Hashtable<String, UserItem>();
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

    public UserItem queryUser(String name) throws IllegalArgumentException, WmiException {
	String upn = toUserPrincipalName(name);
	UserItem item = users.get(upn.toUpperCase());
	if (item == null) {
	    StringBuffer wql = new StringBuffer(USER_WQL);
	    wql.append(" WHERE ");
	    wql.append(USER_WQL_UPN_CONDITION.replaceAll("(?i)\\$upn", Matcher.quoteReplacement(upn)));
	    ISWbemObjectSet os = wmi.execQuery(AD_NAMESPACE, wql.toString());
	    if (os.getSize() == 0) {
		item = JOVALSystem.factories.sc.windows.createUserItem();
		EntityItemStringType user = JOVALSystem.factories.sc.core.createEntityItemStringType();
		user.setValue(name);
		item.setUser(user);
		item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		item = makeItem(name, os.iterator().next().getProperties());
	    }
	    users.put(upn.toUpperCase(), item);
	}
	return item;
    }

    /**
     * This can be an extremely expensive call.  Gets all the users from a domain whose name matches the pattern.
     */
    public List<UserItem> queryAllUsers(Pattern domainPattern) throws WmiException {
	for (ISWbemObject row : wmi.execQuery(AD_NAMESPACE, USER_WQL)) {
	    ISWbemPropertySet columns = row.getProperties();
	    String upn = columns.getItem("DS_userPrincipalName").getValueAsString();
	    if (users.get(upn.toUpperCase()) == null) {
		int ptr = upn.indexOf("@");
		String cn = upn.substring(0, ptr);
		String dns = upn.substring(ptr+1);
		String domain = toDomain(dns);
		if (domain != null) {
		    String name = domain + "\\" + cn;
		    if (domainPattern.matcher(domain).find()) {
			JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_AD_USER_ADD", name));
			users.put(upn.toUpperCase(), makeItem(name, columns));
		    } else {
			JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_AD_USER_SKIP", name));
		    }
		}
	    }
	}
	List<UserItem> items = new Vector<UserItem>();
	for (UserItem item : users.values()) {
	    if (domainPattern.matcher(getDomain((String)item.getUser().getValue())).find()) {
		items.add(item);
	    }
	}
	return items;
    }

    // Private

    /**
     * Given a name of the form DOMAIN\\username, return the corresponding UserPrincipalName of the form username@domain.com.
     */
    private String toUserPrincipalName(String name) throws IllegalArgumentException {
	String dns = domains.get(getDomain(name).toUpperCase());
	if (dns == null) {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_AD_DOMAIN_UNKNOWN", getDomain(name)));
	} else {
	    String upn = new StringBuffer(getUser(name)).append("@").append(dns).toString();
	    JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_UPN_CONVERT", name, upn));
	    return upn;
	}
    }

    private String toDomain(String dns) {
	for (String domain : domains.keySet()) {
	    if (dns.equals(domains.get(domain))) {
		return domain;
	    }
	}
	return null;
    }

    /**
     * Get the Domain portion of a Domain\\User String.
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
     * Get the User portion of a Domain\\User String.  If there is no domain portion, throws an exception.
     */
    private String getUser(String s) throws IllegalArgumentException {
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
    private List<String> parseGroups(String s) {
	List<String> groups = new Vector<String>();

	for (String dn : StringTools.toList(StringTools.tokenize(s, " CN="))) {
	    int ptr = dn.indexOf(",");
	    String groupName = dn.substring(0, ptr);
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

    private UserItem makeItem(String name, ISWbemPropertySet props) throws WmiException {
	int uac = props.getItem("DS_userAccountControl").getValueAsInteger().intValue();
	boolean enabled = 0x00000002 != (uac & 0x00000002); //0x02 flag indicates disabled
	String groups = props.getItem("DS_memberOf").getValueAsString();

	UserItem item = JOVALSystem.factories.sc.windows.createUserItem();
	EntityItemStringType user = JOVALSystem.factories.sc.core.createEntityItemStringType();
	user.setValue(name);
	item.setUser(user);
	EntityItemBoolType enabledType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	enabledType.setValue(enabled ? "true" : "false");
	enabledType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setEnabled(enabledType);

	for (String group : parseGroups(groups)) {
	    EntityItemStringType groupType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    groupType.setValue(group);
	    item.getGroup().add(groupType);
	}

	return item;
    }
}
