// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity;

import java.io.File;

import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.userauth.Identity;
import org.vngx.jsch.userauth.IdentityManager;
import org.vngx.jsch.userauth.IdentityFile;

import org.joval.intf.identity.ICredential;

/**
 * A representation of an abstract credential.  This can either be a username/password pair, or a username, certificate
 * file and cert passphrase.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Credential implements ICredential {
    protected String username;
    protected String password;
    protected String passphrase;

    public Credential() {
	username = null;
	password = null;
	passphrase = null;
    }

    /**
     * Create a Credential using a username and password.
     */
    public Credential(String username, String password) {
	this.username = username;
	this.password = password;
    }

    /**
     * Create a Credential for a certificate.
     */
    public Credential(String username, File privateKey, String passphrase) throws JSchException {
	this.username = username;
	Identity id = IdentityFile.newInstance(privateKey.getPath(), null);
	IdentityManager.getManager().addIdentity(id, passphrase.getBytes());
	this.passphrase = passphrase;
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

    public String getPassphrase() {
	return passphrase;
    }
}
