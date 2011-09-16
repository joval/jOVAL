// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.vngx.jsch.exception.JSchException;

import org.joval.discovery.SessionFactory;
import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.unix.remote.system.UnixSession;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.ssh.identity.SshCredential;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Implementation of an IJovaldiPlugin for remote scanning.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RemotePlugin extends OnlinePlugin {
    private String hostname;
    private ICredential cred;
    private SessionFactory sessionFactory;

    /**
     * Create a remote plugin.
     */
    public RemotePlugin() {
	super();
    }

    // Implement IJovaldiPlugin

    public void setDataDirectory(File dir) {
	try {
	    sessionFactory = new SessionFactory(dir);
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Create a plugin for data retrieval and test evaluation.
     */
    public boolean configure(Properties props) {
	if (props == null) {
	    return false;
	}

	String hostname		= props.getProperty("hostname");
	String domain		= props.getProperty("nt.domain");
	String username		= props.getProperty("user.name");
	String password		= props.getProperty("user.password");
	String passphrase	= props.getProperty("key.password");
	String rootPassword	= props.getProperty("root.password");
	String privateKey	= props.getProperty("key.file");

	if (hostname != null) {
	    try {
		IBaseSession base = sessionFactory.createSession(hostname);
		ICredential cred = null;
		if (base instanceof ILocked) {
		    if (username == null) {
			err = getMessage("ERROR_USERNAME");
			return false;
		    } else if (privateKey == null && password == null) {
			err = getMessage("ERROR_PASSWORD");
			return false;
		    }
		    File privateKeyFile = null;
		    if (privateKey != null) {
			privateKeyFile = new File(privateKey);
			if (!privateKeyFile.isFile()) {
			    err = getMessage("ERROR_PRIVATE_KEY", privateKeyFile.getPath());
			    return false;
			}
		    }
    
		    switch (base.getType()) {
		      case WINDOWS:
			if (domain == null) {
			    domain = hostname;
			}
			cred = new WindowsCredential(domain, username, password);
			break;
    
		      case SSH: 
			if (privateKeyFile != null) {
			    try {
				cred = new SshCredential(username, privateKeyFile, passphrase, rootPassword);
			    } catch (JSchException e) {
				err = getMessage("ERROR_PRIVATE_KEY", e.getMessage());
				return false;
			    }
			} else if (rootPassword != null) {
			    cred = new SshCredential(username, password, rootPassword);
			} else {
			    cred = new Credential(username, password);
			}
			break;
		    }
    
		    if (!((ILocked)base).unlock(cred)) {
			err = getMessage("ERROR_LOCK", cred.getClass().getName(), session.getClass().getName());
			return false;
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
		    break;
	        }

		if (session instanceof ILocked) {
		    ((ILocked)session).unlock(cred);
		}
	    } catch (Exception e) {
		err = e.getMessage();
		return false;
	    }

	    return true;
	} else {
	    err = getMessage("ERROR_HOSTNAME");
	    return false;
	}
    }
}
