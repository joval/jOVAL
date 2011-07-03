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
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.di.BasePlugin;
import org.joval.util.JOVALSystem;
import org.joval.windows.remote.WindowsCredential;

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
     *
     * A note about the redirect64 setting: If true, then registry and filesystem redirection will be active for 64-bit
     * machines.  This means that redirected assets (files, registry entries) will appear to be in their 32-bit locations.
     * Hence, if you intend to run tests that specifically look for assets in their REAL 64-bit locations this should be set
     * to false.  If you intend to run tests that were designed for 32-bit Windows systems, then set this parameter to true.
     */
    public boolean configure(String[] args, IJovaldiConfiguration jDIconfig) {
	if (jDIconfig.printingHelp()) return true;

	String hostname = null;
	boolean redirect64 = true;
	String domain=null, username=null, password=null, passphrase=null;
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
	    } else if (args[i].equals("-redirect64")) {
		redirect64 = "true".equals(args[++i]);
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
		session = sessionFactory.createSession(hostname);
	    } catch (UnknownHostException e) {
		err = e.getMessage();
		return false;
	    }

	    if (session.getType() == ISession.WINDOWS) {
		((IWindowsSession)session).set64BitRedirect(redirect64);
	    }
	    if (session instanceof ILocked) {
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
		ICredential cred;
		if (session.getType() == ISession.WINDOWS) {
		    if (domain == null) {
			domain = hostname;
		    }
		    cred = new WindowsCredential(domain, username, password);
		} else if (privateKey != null) {
		    try {
			cred = new Credential(username, privateKey, passphrase);
		    } catch (JSchException e) {
			err = getMessage("ERROR_PRIVATE_KEY", e.getMessage());
			return false;
		    }
		} else {
		    cred = new Credential(username, password);
		}
		if (((ILocked)session).unlock(cred)) {
		    return true;
		} else {
		    err = getMessage("ERROR_LOCK", cred.getClass().getName(), session.getClass().getName());
		    return false;
		}
	    }
	    return true;
	} else {
	    err = getMessage("ERROR_HOSTNAME");
	    return false;
	}
    }
}
