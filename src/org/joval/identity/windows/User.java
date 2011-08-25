// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity.windows;

import java.util.List;

/**
 * The User class stores information about a Windows user.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class User {
    private String domain, name, sid;
    private boolean enabled;
    private List<String> groupNetbiosNames;

    User(String domain, String name, String sid, List<String> groupNetbiosNames, boolean enabled) {
	this.domain = domain;
	this.name = name;
	this.sid = sid;
	this.groupNetbiosNames = groupNetbiosNames;
	this.enabled = enabled;
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

    public List<String> getGroupNetbiosNames() {
	return groupNetbiosNames;
    }

    public boolean isEnabled() {
	return enabled;
    }
}
