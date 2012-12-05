// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
     * @see http://msdn.microsoft.com/en-us/library/cc980032%28v=prot.20%29.aspx
     */
    static final Map<String, String> BUILTIN_STATIC;
    static final Map<Pattern, String> BUILTIN_PATTERN;
    static {
	HashMap<String, String> sidMap = new HashMap<String, String>();
	sidMap.put("S-1-0", "Null Authority");
	sidMap.put("S-1-0-0", "Nobody");
	sidMap.put("S-1-1", "World Authority");
	sidMap.put("S-1-1-0", "Everyone");
	sidMap.put("S-1-2", "Local Authority");
	sidMap.put("S-1-2-1", "Console Login");
	sidMap.put("S-1-3", "Creator Authority");
	sidMap.put("S-1-3-0", "Creator Owner");
	sidMap.put("S-1-3-1", "Creator Group");
	sidMap.put("S-1-3-2", "Creator Owner Server");
	sidMap.put("S-1-3-3", "Creator Group Server");
	sidMap.put("S-1-3-4", "Owner Rights");
	sidMap.put("S-1-5-80-0", "All Services");
	sidMap.put("S-1-4", "Non-unique Authority");
	sidMap.put("S-1-5", "NT Authority");
	sidMap.put("S-1-5-1", "Dialup");
	sidMap.put("S-1-5-2", "Network");
	sidMap.put("S-1-5-3", "Batch");
	sidMap.put("S-1-5-4", "Interactive");
	sidMap.put("S-1-5-6", "Service");
	sidMap.put("S-1-5-7", "Anonymous");
	sidMap.put("S-1-5-8", "Proxy");
	sidMap.put("S-1-5-9", "Enterprise Domain Controllers");
	sidMap.put("S-1-5-10", "Principal Self");
	sidMap.put("S-1-5-11", "Authenticated Users");
	sidMap.put("S-1-5-12", "Restricted Code");
	sidMap.put("S-1-5-13", "Terminal Server Users");
	sidMap.put("S-1-5-14", "Remote Interactive Logon");
	sidMap.put("S-1-5-15", "This Organization");
	sidMap.put("S-1-5-17", "IUSR");
	sidMap.put("S-1-5-18", "Local System");
	sidMap.put("S-1-5-19", "Local Service");
	sidMap.put("S-1-5-20", "NT Authority");

	sidMap.put("S-1-5-80", "NT Service");
	sidMap.put("S-1-5-80-0", "NT SERVICES\\ALL SERVICES");
	sidMap.put("S-1-5-80-956008885-3418522649-1831038044-1853292631-2271478464", "NT SERVICE\\TrustedInstaller");

	sidMap.put("S-1-5-84-0-0-0-0-0", "User Mode Drivers");
	sidMap.put("S-1-5-1000", "Other Organization");

	sidMap.put("S-1-5-32", "BUILTIN");
	sidMap.put("S-1-5-32-544", "Administrators");
	sidMap.put("S-1-5-32-545", "Users");
	sidMap.put("S-1-5-32-546", "Guests");
	sidMap.put("S-1-5-32-547", "Power Users");
	sidMap.put("S-1-5-32-548", "Account Operators");
	sidMap.put("S-1-5-32-549", "Server Operators");
	sidMap.put("S-1-5-32-550", "Print Operators");
	sidMap.put("S-1-5-32-551", "Backup Operators");
	sidMap.put("S-1-5-32-552", "Replicators");
	sidMap.put("S-1-5-32-554", "BUILTIN\\Pre-Windows 2000 Compatible-Access");
	sidMap.put("S-1-5-32-555", "BUILTIN\\Remote Desktop Users");
	sidMap.put("S-1-5-32-556", "BUILTIN\\Network Configuration Operators");
	sidMap.put("S-1-5-32-557", "BUILTIN\\Incoming Forest Trust Users");
	sidMap.put("S-1-5-32-558", "BUILTIN\\Performance Monitor Users");
	sidMap.put("S-1-5-32-559", "BUILTIN\\Performance Log Users");
	sidMap.put("S-1-5-32-560", "BUILTIN\\Windows Authorization Access Group");
	sidMap.put("S-1-5-32-561", "BUILTIN\\Terminal Server License Servers");
	sidMap.put("S-1-5-32-562", "BUILTIN\\Distributed COM Servers");
	sidMap.put("S-1-5-32-568", "IIS_USRS");
	sidMap.put("S-1-5-32-569", "BUILTIN\\Cryptographic Operators");
	sidMap.put("S-1-5-32-573", "BUILTIN\\Event Log Readers");
	sidMap.put("S-1-5-32-574", "BUILTIN\\Certificate Service DCOM Access");
	sidMap.put("S-1-5-32-575", "BUILTIN\\RDS Remote Access Servers");
	sidMap.put("S-1-5-32-576", "BUILTIN\\RDS Remote Endpoint Servers");
	sidMap.put("S-1-5-32-577", "BUILTIN\\RDS Management Servers");
	sidMap.put("S-1-5-32-578", "BUILTIN\\Hyper-V Administrators");
	sidMap.put("S-1-5-32-579", "BUILTIN\\Access Control Assistance Operators");
	sidMap.put("S-1-5-32-580", "BUILTIN\\Remote Management Users");

	sidMap.put("S-1-5-64-10", "NTLM Authentication");
	sidMap.put("S-1-5-64-14", "SChannel Authentication");
	sidMap.put("S-1-5-64-21", "Digest Authentication");

	sidMap.put("S-1-15-2-1", "All App Packages");
	sidMap.put("S-1-16-0", "Untrusted Mandatory Level");
	sidMap.put("S-1-16-4096", "Low Mandatory Level");
	sidMap.put("S-1-16-8192", "Medium Mandatory Level");
	sidMap.put("S-1-16-8448", "Medium Plus Mandatory Level");
	sidMap.put("S-1-16-12288", "High Mandatory Level");
	sidMap.put("S-1-16-16384", "System Mandatory Level");
	sidMap.put("S-1-16-20480", "Protected Process Mandatory Level");
	sidMap.put("S-1-16-28672", "Secure Process Mandatory Level");

	sidMap.put("S-1-18-1", "Authentication Authority Asserted Identity");
	sidMap.put("S-1-18-2", "Service Asserted Identity");

	BUILTIN_STATIC = Collections.unmodifiableMap(sidMap);

	HashMap<Pattern, String> patternMap = new HashMap<Pattern, String>();
	try {
	    patternMap.put(Pattern.compile("^S-1-5-5-\\d+-\\d+$"), "Logon Session");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-500$"), "Logon Session");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-501$"), "Guest");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-502$"), "KRBTGT");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-512$"), "Domain Admins");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-513$"), "Domain Users");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-514$"), "Domain Guests");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-515$"), "Domain Computers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-516$"), "Domain Controllers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-517$"), "Cert Publishers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-518$"), "Schema Admins");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-519$"), "Enterprise Admins");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-520$"), "Group Policy Creator Owners");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-553$"), "RAS and IAS Servers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-498$"), "Enterprise Read-Only Comain Controllers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-521$"), "Read-Only Domain Controllers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-571$"), "Allowed RODC Password Replication Group");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-572$"), "Denied RODC Password Replication Group");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-522$"), "Cloneable Domain Controllers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-1000$"), "HomeUsers");
	    patternMap.put(Pattern.compile("^S-1-5-21-[0-9\\-]+-1002$"), "HomeGroupUser$");
	} catch (PatternSyntaxException e) {
	    e.printStackTrace();
	}
	BUILTIN_PATTERN = Collections.unmodifiableMap(patternMap);
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
	if (BUILTIN_STATIC.containsKey(sid)) {
	    return true;
	} else {
	    for (Pattern p : BUILTIN_PATTERN.keySet()) {
		if (p.matcher(sid).find()) {
		    return true;
		}
	    }
	}
	return false;
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
