// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;

/**
 * The Group class stores information about a Windows group.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Group extends Principal {
    private Collection<String> memberUserNetbiosNames;
    private Collection<String> memberGroupNetbiosNames;

    Group(String domain, String name, String sid,
	  Collection<String> memberUserNetbiosNames, Collection<String> memberGroupNetbiosNames) {

	super(domain, name, sid);
	this.memberUserNetbiosNames = memberUserNetbiosNames;
	this.memberGroupNetbiosNames = memberGroupNetbiosNames;
    }

    /**
     * Non-recursive.
     */
    public Collection<String> getMemberUserNetbiosNames() {
	return memberUserNetbiosNames;
    }

    /**
     * Non-recursive.
     */
    public Collection<String> getMemberGroupNetbiosNames() {
	return memberGroupNetbiosNames;
    }

    public Type getType() {
	return Type.GROUP;
    }
}
