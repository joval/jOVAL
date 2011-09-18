// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity;

import org.joval.intf.identity.ICredential;

/**
 * A representation of an abstract credential, consisting of a username and password.  Subclasses include WindowsCredential
 * (which adds a DOMAIN), and SSHCredential (which adds a root password, private key file and key file passphrase).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Credential implements ICredential {
    protected String username;
    protected String password;

    public Credential() {
	username = null;
	password = null;
    }

    /**
     * Create a Credential using a username and password.
     */
    public Credential(String username, String password) {
	this.username = username;
	this.password = password;
    }

    public void setUsername(String username) {
	this.username = username;
    }

    public void setPassword(String password) {
	this.password = password;
    }

    // Implement ICredential

    public String getUsername() {
	return username;
    }

    public String getPassword() {
	return password;
    }
}
