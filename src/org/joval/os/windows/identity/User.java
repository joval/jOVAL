// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;

/**
 * The User class stores information about a Windows user.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class User extends Principal {
    private boolean enabled;
    private Collection<String> groupNetbiosNames;

    User(String domain, String name, String sid, Collection<String> groupNetbiosNames, boolean enabled) {
	super(domain, name, sid);
	this.groupNetbiosNames = groupNetbiosNames;
	this.enabled = enabled;
    }

    public Collection<String> getGroupNetbiosNames() {
	return groupNetbiosNames;
    }

    public boolean isEnabled() {
	return enabled;
    }

    public Type getType() {
	return Type.USER;
    }
}
