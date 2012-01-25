// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.ssh.identity;

import java.io.File;

import org.joval.identity.Credential;
import org.joval.intf.ssh.identity.ISshCredential;

/**
 * A representation of a Unix credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SshCredential extends Credential implements ISshCredential {
    private String passphrase;
    private String rootPassword;
    private File privateKey;

    public SshCredential(String username, String password, String rootPassword) {
	super(username, password);
	this.rootPassword = rootPassword;
    }

    /**
     * Create a Credential for a certificate.
     */
    public SshCredential(String username, File privateKey, String passphrase, String rootPassword) {
	this(username, null, rootPassword);
	this.passphrase = passphrase;
	this.privateKey = privateKey;
    }

    // Implement ISshCredential

    public String getRootPassword() {
	return rootPassword;
    }

    public String getPassphrase() {
	return passphrase;
    }

    public File getPrivateKey() {
	return privateKey;
    }
}
