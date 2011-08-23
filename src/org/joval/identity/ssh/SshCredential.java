// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.identity.ssh;

import java.io.File;

import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.userauth.Identity;
import org.vngx.jsch.userauth.IdentityManager;
import org.vngx.jsch.userauth.IdentityFile;

import org.joval.identity.Credential;
import org.joval.util.JOVALSystem;

/**
 * A representation of a Unix credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SshCredential extends Credential {
    private String passphrase;
    private String rootPassword;

    public SshCredential(String username, String password, String rootPassword) {
	super(username, password);
	this.rootPassword = rootPassword;
    }

    /**
     * Create a Credential for a certificate.
     */
    public SshCredential(String username, File privateKey, String passphrase, String rootPassword) throws JSchException {
        this.username = username;
        Identity id = IdentityFile.newInstance(privateKey.getPath(), null);
        if (passphrase == null) {
            IdentityManager.getManager().addIdentity(id, null);
        } else {
            IdentityManager.getManager().addIdentity(id, passphrase.getBytes());
        }
        this.passphrase = passphrase;
	this.rootPassword = rootPassword;
    }

    public String getRootPassword() {
	return rootPassword;
    }

    public String getPassphrase() {
	return passphrase;
    }
}
