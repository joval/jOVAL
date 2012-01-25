// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.os.windows.identity;

import org.joval.identity.Credential;
import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A representation of a Windows domain credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsCredential extends Credential implements IWindowsCredential {
    private String domain;

    /**
     * Create a Credential from a String of the form [DOMAIN\\]username:password.
     */
    public WindowsCredential(String data) {
	int ptr = data.indexOf("\\");
	if (ptr > 0) {
	    domain = data.substring(0, ptr);
	    data = data.substring(ptr+1);
	}
	ptr = data.indexOf(":");
	if (ptr > 0) {
	    username = data.substring(0, ptr);
	    password = data.substring(ptr+1);
	} else {
	    username = data;
	}
    }

    public WindowsCredential(String domain, String username, String password) {
	super(username, password);
	this.domain = domain;
	JOVALSystem.getLogger().trace(JOVALMsg.STATUS_WINCRED_CREATE, getDomainUser());
    }

    // Implement IWindowsCredential

    /**
     * Return a username of the form domain\\name.
     */
    public String getDomainUser() {
	return new StringBuffer(domain).append('\\').append(username).toString();
    }

    public String getDomain() {
	return domain;
    }

    public void setDomain(String domain) {
	this.domain = domain;
    }
}
