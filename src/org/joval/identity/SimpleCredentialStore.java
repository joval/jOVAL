// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.identity;

import java.io.File;
import java.security.AccessControlException;
import java.util.Hashtable;
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
 * Trivial implementation of an ICredentialStore that contains credentials keyed by hostname.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SimpleCredentialStore implements ICredentialStore {
    public static final String PROP_HOSTNAME		= "hostname";
    public static final String PROP_DOMAIN		= "nt.domain";
    public static final String PROP_USERNAME		= "user.name";
    public static final String PROP_PASSWORD		= "user.password";
    public static final String PROP_PASSPHRASE		= "key.password";
    public static final String PROP_ROOT_PASSWORD	= "root.password";
    public static final String PROP_PRIVATE_KEY		= "key.file";

    private Hashtable<String, Properties> table;

    /**
     * Create from Properties.
     */
    public SimpleCredentialStore() {
	table = new Hashtable<String, Properties>();
    }

    /**
     * Add properties for a credential.
     *
     * @throws IllegalArgumentException if a PROP_HOSTNAME is not specified.
     */
    public void add(Properties props) throws IllegalArgumentException {
	String hostname = props.getProperty(PROP_HOSTNAME);
	if (hostname == null) {
	    throw new IllegalArgumentException(PROP_HOSTNAME);
	} else {
	    table.put(hostname, props);
	}
    }

    // Implement ICredentialStore

    public ICredential getCredential(IBaseSession base) throws AccessControlException {
	Properties props = table.get(base.getHostname());
	if (props == null) {
	    return null;
	}
	String domain		= props.getProperty(PROP_DOMAIN);
	String username		= props.getProperty(PROP_USERNAME);
	String password		= props.getProperty(PROP_PASSWORD);
	String passphrase	= props.getProperty(PROP_PASSPHRASE);
	String rootPassword	= props.getProperty(PROP_ROOT_PASSWORD);
	String privateKey	= props.getProperty(PROP_PRIVATE_KEY);

	ICredential cred = null;
	if (base.getHostname().equalsIgnoreCase(props.getProperty(PROP_HOSTNAME))) {
	    switch (base.getType()) {
	      case WINDOWS:
		if (domain == null) {
		    domain = props.getProperty(PROP_HOSTNAME).toUpperCase();
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
