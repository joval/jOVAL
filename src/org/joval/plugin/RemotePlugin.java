// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;

import org.vngx.jsch.exception.JSchException;

import org.joval.discovery.SessionFactory;
import org.joval.identity.Credential;
import org.joval.intf.di.IJovaldiConfiguration;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.unix.remote.system.UnixSession;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.oval.di.BasePlugin;
import org.joval.ssh.identity.SshCredential;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALSystem;

/**
 * Implementation of an IJovaldiPlugin for the Windows operating system.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RemotePlugin extends BasePlugin {
    private String hostname;
    private ICredential cred;
    private SessionFactory sessionFactory;

    /**
     * Create a plugin solely for test evaluation.
     */
    public RemotePlugin() {
	super();
    }

    // Implement IJovaldiPlugin

    public void setDataDirectory(File dir) {
	try {
	    sessionFactory = new SessionFactory(dir);
	} catch (IOException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	}
    }

    /**
     * Create a plugin for data retrieval and test evaluation.
     */
    public boolean configure(String[] args, IJovaldiConfiguration jDIconfig) {
	if (jDIconfig.printingHelp()) return true;

	String hostname = null;
	String domain=null, username=null, password=null, passphrase=null, rootPassword=null;
	File privateKey = null;

	for (int i=0; i < args.length; i++) {
	    if (args[i].equals("-hostname")) {
		hostname = args[++i];
	    } else if (args[i].equals("-domain")) {
		domain = args[++i];
	    } else if (args[i].equals("-username")) {
		username = args[++i];
	    } else if (args[i].equals("-password")) {
		password = args[++i];
	    } else if (args[i].equals("-privateKey")) {
		privateKey = new File(args[++i]);
	    } else if (args[i].equals("-passphrase")) {
		passphrase = args[++i];
	    } else if (args[i].equals("-rootPassword")) {
		rootPassword = args[++i];
	    }
	}

	//
	// Configure for analysis only.
	//
	if (jDIconfig.getSystemCharacteristicsInputFile() != null && hostname == null) {
	    session = null;
	    return true;

	//
	// Configure for both scanning and analysis.
	//
	} else if (hostname != null) {
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
		    } else if (privateKey != null && !privateKey.isFile()) {
			err = getMessage("ERROR_PRIVATE_KEY", privateKey.getPath());
			return false;
		    }
    
		    switch (base.getType()) {
		      case WINDOWS:
			if (domain == null) {
			    domain = hostname;
			}
			cred = new WindowsCredential(domain, username, password);
			break;
    
		      case SSH: 
			if (privateKey != null) {
			    try {
				cred = new SshCredential(username, privateKey, passphrase, rootPassword);
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
System.out.println("DAS: screwed: " + base.getType());
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
