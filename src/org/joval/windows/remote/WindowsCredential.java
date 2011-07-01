// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.windows.remote;

import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import org.jinterop.dcom.common.IJIAuthInfo;

import org.joval.identity.Credential;
import org.joval.util.JOVALSystem;

/**
 * A representation of a Windows domain credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsCredential extends Credential implements IJIAuthInfo {
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
	JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_WINCRED_CREATE", getDomainUser()));
    }

    /**
     * Return a username of the form domain\\name.
     */
    public String getDomainUser() {
	return new StringBuffer(domain).append('\\').append(username).toString();
    }

    public void setDomain(String domain) {
	this.domain = domain;
    }

    public NtlmPasswordAuthentication getNtlmPasswordAuthentication() {
	return new NtlmPasswordAuthentication(domain, username, password);
    }

    // Implement IJIAuthInfo

    public String getUserName() {
	return username;
    }

    public String getDomain() {
	return domain;
    }
}
