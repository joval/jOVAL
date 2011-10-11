// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.List;

import org.joval.intf.windows.identity.IPrincipal;

/**
 * The abstract parent class for User and Group.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
abstract class Principal implements IPrincipal {
    String domain, name, sid;

    Principal(String domain, String name, String sid) {
	this.domain = domain;
	this.name = name;
	this.sid = sid;
    }

    // Implement IPrincipal

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
