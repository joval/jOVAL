// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity;

import java.io.File;
import java.security.AccessControlException;
import java.util.Properties;

import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ICredentialStore;
import org.joval.intf.system.IBaseSession;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.ssh.identity.SshCredential;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Trivial implementation of an ICredentialStore that contains only one credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SimpleCredentialStore implements ICredentialStore {
    private Properties props;

    /**
     * Create from Properties.
     */
    public SimpleCredentialStore(Properties props) {
	this.props = props;
    }

    // Implement ICredentialStore

    public ICredential getCredential(IBaseSession base) throws AccessControlException {
	ICredential cred = null;
	if (props == null) {
	    return null;
	}
	String hostname		= props.getProperty("hostname");
	String domain		= props.getProperty("nt.domain");
	String username		= props.getProperty("user.name");
	String password		= props.getProperty("user.password");
	String passphrase	= props.getProperty("key.password");
	String rootPassword	= props.getProperty("root.password");
	String privateKey	= props.getProperty("key.file");

	if (base.getHostname().equalsIgnoreCase(hostname) && props != null) {
	    switch (base.getType()) {
	      case WINDOWS:
		if (domain == null) {
		    domain = hostname;
		}
		cred = new WindowsCredential(domain, username, password);
		break;
   
	      default:
		if (privateKey != null) {
		    cred = new SshCredential(username, new File(privateKey), passphrase, rootPassword);
		} else if (rootPassword != null) {
		    cred = new SshCredential(username, password, rootPassword);
		} else if (username != null && password != null) {
		    cred = new Credential(username, password);
		} else {
		    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_SESSION_CREDENTIAL_PASSWORD, username);
		}
		break;
	    }
	}
	return cred;
    }
}
