// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.joval.intf.windows.identity.IPrincipal;

/**
 * The abstract parent class for User and Group.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
abstract class Principal implements IPrincipal {
    /**
     * All of the Principals with fixed SIDs.
     * @see http://support.microsoft.com/kb/243330?wa=wsignin1.0
     */
    static final Map<String, String> BUILTIN;
    static {
	HashMap<String, String> map = new HashMap<String, String>();
	map.put("S-1-0", "Null Authority");
	map.put("S-1-0-0", "Nobody");
	map.put("S-1-1", "World Authority");
	map.put("S-1-1-0", "Everyone");
	map.put("S-1-2", "Local Authority");
	map.put("S-1-2-1", "Console Login");
	map.put("S-1-3", "Creator Authority");
	map.put("S-1-3-1", "Creator Group");
	map.put("S-1-3-2", "Creator Owner Server");
	map.put("S-1-3-3", "Creator Group Server");
	map.put("S-1-3-4", "Owner Rights");
	map.put("S-1-5-80-0", "All Services");
	map.put("S-1-4", "Non-unique Authority");
	map.put("S-1-5", "NT Authority");
	map.put("S-1-5-1", "Dialup");
	map.put("S-1-5-2", "Network");
	map.put("S-1-5-3", "Batch");
	map.put("S-1-5-4", "Interactive");
	map.put("S-1-5-6", "Service");
	map.put("S-1-5-7", "Anonymous");
	map.put("S-1-5-8", "Proxy");
	map.put("S-1-5-9", "Enterprise Domain Controllers");
	map.put("S-1-5-10", "Principal Self");
	map.put("S-1-5-11", "Authenticated Users");
	map.put("S-1-5-12", "Restricted Code");
	map.put("S-1-5-13", "Terminal Server Users");
	map.put("S-1-5-14", "Remote Interactive Logon");
	map.put("S-1-5-15", "This Organization");
	map.put("S-1-5-17", "This Organization");
	map.put("S-1-5-18", "Local System");
	map.put("S-1-5-19", "Local Service");
	map.put("S-1-5-20", "NT Authority");
	map.put("S-1-5-32-544", "Administrators");
	map.put("S-1-5-32-545", "Users");
	map.put("S-1-5-32-546", "Guests");
	map.put("S-1-5-32-547", "Power Users");
	map.put("S-1-5-32-548", "Account Operators");
	map.put("S-1-5-32-549", "Server Operators");
	map.put("S-1-5-32-550", "Print Operators");
	map.put("S-1-5-32-551", "Backup Operators");
	map.put("S-1-5-32-552", "Replicators");
	map.put("S-1-5-64-10", "NTLM Authentication");
	map.put("S-1-5-64-14", "SChannel Authentication");
	map.put("S-1-5-64-21", "Digest Authentication");
	map.put("S-1-5-80", "NT Service");
	map.put("S-1-16-0", "Untrusted Mandatory Level");
	map.put("S-1-16-4096", "Low Mandatory Level");
	map.put("S-1-16-8192", "Medium Mandatory Level");
	map.put("S-1-16-8448", "Medium Plus Mandatory Level");
	map.put("S-1-16-12288", "High Mandatory Level");
	map.put("S-1-16-16384", "System Mandatory Level");
	map.put("S-1-16-20480", "Protected Process Mandatory Level");
	map.put("S-1-16-28672", "Secure Process Mandatory Level");
	map.put("S-1-5-80-0", "NT SERVICES\\ALL SERVICES");
	map.put("S-1-5-32-554", "BUILTIN\\Pre-Windows 2000 Compatible-Access");
	map.put("S-1-5-32-555", "BUILTIN\\Remote Desktop Users");
	map.put("S-1-5-32-556", "BUILTIN\\Network Configuration Operators");
	map.put("S-1-5-32-557", "BUILTIN\\Incoming Forest Trust Users");
	map.put("S-1-5-32-558", "BUILTIN\\Performance Monitor Users");
	map.put("S-1-5-32-559", "BUILTIN\\Performance Log Users");
	map.put("S-1-5-32-560", "BUILTIN\\Windows Authorization Access Group");
	map.put("S-1-5-32-561", "BUILTIN\\Terminal Server License Servers");
	map.put("S-1-5-32-562", "BUILTIN\\Distributed COM Servers");
	map.put("S-1-5-32-569", "BUILTIN\\Cryptographic Operators");
	map.put("S-1-5-32-573", "BUILTIN\\Event Log Readers");
	map.put("S-1-5-32-574", "BUILTIN\\Certificate Service DCOM Access");
	map.put("S-1-5-32-575", "BUILTIN\\RDS Remote Access Servers");
	map.put("S-1-5-32-576", "BUILTIN\\RDS Remote Endpoint Servers");
	map.put("S-1-5-32-577", "BUILTIN\\RDS Management Servers");
	map.put("S-1-5-32-578", "BUILTIN\\Hyper-V Administrators");
	map.put("S-1-5-32-579", "BUILTIN\\Access Control Assistance Operators");
	map.put("S-1-5-32-580", "BUILTIN\\Remote Management Users");
	BUILTIN = Collections.unmodifiableMap(map);
    }

    String domain, name, sid;

    Principal(String domain, String name, String sid) {
	this.domain = domain;
	this.name = name;
	this.sid = sid;
    }

    public boolean equals(Object other) {
	if (other instanceof IPrincipal) {
	    return sid.equals(((IPrincipal)other).getSid());
	} else {
	    return super.equals(other);
	}
    }

    // Implement IPrincipal

    public boolean isBuiltin() {
	return BUILTIN.containsKey(sid);
    }

    public String getNetbiosName() {
	return domain + "\\" + name;
    }

    public String getDomain() {
	return domain;
    }

    public String getName() {
	return name;
    }

    public String getSid() {
	return sid;
    }

    public abstract Type getType();
}
