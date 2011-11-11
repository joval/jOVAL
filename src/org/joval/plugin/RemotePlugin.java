// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.vngx.jsch.exception.JSchException;

import org.joval.discovery.SessionFactory;
import org.joval.identity.Credential;
import org.joval.intf.discovery.ISessionFactory;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.unix.remote.system.UnixSession;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.oval.OvalException;
import org.joval.ssh.identity.SshCredential;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Implementation of an IPlugin for remote scanning.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RemotePlugin extends BasePlugin {
    private String hostname;
    private ICredential cred;

    /**
     * Create a remote plugin.
     */
    public RemotePlugin() {
	super();
    }

    // Implement IPlugin

    public void setTarget(String hostname) {
	this.hostname = hostname;
    }

    public void connect() throws OvalException {
	if (hostname != null) {
	    try {
		IBaseSession base = sessionFactory.createSession(hostname);
		ICredential cred = JOVALSystem.getCredentialStore().getCredential(base);
		if (base instanceof ILocked) {
		    if (!((ILocked)base).unlock(cred)) {
			String baseName = base.getClass().getName();
			String credName = cred.getClass().getName();
			throw new Exception(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_LOCK, credName, baseName));
		    }
		}

		switch (base.getType()) {
		  case WINDOWS:
		    session = (IWindowsSession)base;
		    break;

		  case UNIX:
		    base.disconnect();
		    UnixSession us = new UnixSession(new SshSession(hostname));
		    us.unlock(cred);
		    session = us;
		    break;

		  case CISCO_IOS:
		    base.disconnect();
		    IosSession is = new IosSession(new SshSession(hostname));
		    is.unlock(cred);
		    session = is;
		    break;

		  default:
		    base.disconnect();
		    throw new Exception(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_TYPE, base.getType()));
	        }

		if (session instanceof ILocked) {
		    ((ILocked)session).unlock(cred);
		}
	    } catch (Exception e) {
		throw new OvalException(e);
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_TARGET));
	}

	super.connect();
    }
}
