// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;

import org.joval.intf.windows.identity.IUser;

/**
 * The User class stores information about a Windows user.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class User extends Principal implements IUser {
    private boolean enabled;
    private Collection<String> groupNetbiosNames;

    User(String domain, String name, String sid, Collection<String> groupNetbiosNames, boolean enabled) {
	super(domain, name, sid);
	this.groupNetbiosNames = groupNetbiosNames;
	this.enabled = enabled;
    }

    // Implement IUser

    public Collection<String> getGroupNetbiosNames() {
	return groupNetbiosNames;
    }

    public boolean isEnabled() {
	return enabled;
    }

    // Implement IPrincipal

    public Type getType() {
	return Type.USER;
    }
}
