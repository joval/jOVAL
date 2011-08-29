// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.List;

/**
 * The Group class stores information about a Windows group.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Group {
    private String domain, name, sid;
    private List<String> memberUserNetbiosNames;
    private List<String> memberGroupNetbiosNames;

    Group(String domain, String name, String sid, List<String> memberUserNetbiosNames, List<String> memberGroupNetbiosNames) {
	this.domain = domain;
	this.name = name;
	this.sid = sid;
	this.memberUserNetbiosNames = memberUserNetbiosNames;
	this.memberGroupNetbiosNames = memberGroupNetbiosNames;
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

    /**
     * Non-recursive.
     */
    public List<String> getMemberUserNetbiosNames() {
	return memberUserNetbiosNames;
    }

    /**
     * Non-recursive.
     */
    public List<String> getMemberGroupNetbiosNames() {
	return memberGroupNetbiosNames;
    }
}
