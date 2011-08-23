// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.*;

import org.vngx.jsch.JSch;
import org.jinterop.dcom.common.JISystem;

import org.joval.discovery.SessionFactory;
import org.joval.identity.Credential;
import org.joval.identity.ssh.SshCredential;
import org.joval.identity.windows.WindowsCredential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALSystem;
import org.joval.util.JSchLogger;

public class Remote {
    public static void main (String[] argv) {
	try {
	    Properties props = new Properties();
	    if (argv.length == 1) {
		File f = new File(argv[0]);
		FileInputStream in = new FileInputStream(f);
		props.load(in);
		in.close();
	    } else {
		System.exit(1);
	    }

	    if ("true".equals(props.getProperty("joval.verbose"))) {
		JOVALSystem.setInBuiltLogHandler(true);
		JOVALSystem.getLogger().setLevel(Level.FINEST);
	    } else {
		JOVALSystem.getLogger().setLevel(Level.WARNING);
	    }
	    if ("true".equals(props.getProperty("jinterop.verbose"))) {
		JISystem.setInBuiltLogHandler(true);
		JISystem.getLogger().setLevel(Level.FINEST);
	    } else {
		JISystem.getLogger().setLevel(Level.WARNING);
	    }
	    if ("true".equals(props.getProperty("jsch.verbose"))) {
		JSch.setLogger(new JSchLogger(JOVALSystem.getLogger()));
	    }

	    String host = props.getProperty("host");
	    String domain = props.getProperty("domain");
	    String username = props.getProperty("username");
	    String password = props.getProperty("password");
	    String privateKey = props.getProperty("privateKey");
	    String passphrase = props.getProperty("passphrase");
	    String rootPassword = props.getProperty("rootPassword");

	    SessionFactory factory = new SessionFactory(new File("."));
	    ISession session = factory.createSession(host);
	    if (session instanceof ILocked) {
		ILocked locked = (ILocked)session;
		ICredential cred = null;
		switch(session.getType()) {
		  case ISession.UNIX:
		    if (privateKey != null) {
			cred = new SshCredential(username, new File(privateKey), passphrase, rootPassword);
		    } else {
			cred = new SshCredential(username, password, rootPassword);
		    }
		    break;

		  case ISession.WINDOWS:
		    cred = new WindowsCredential(domain, username, password);
		    break;

		  default:
		    cred = new Credential(username, password);
		    break;
		}
		locked.unlock(cred);
	    }
	    if (session.connect()) {
		if ("true".equals(props.getProperty("test.fs"))) {
		    FS fs = new FS(session);
		    fs.test(props.getProperty("fs.path"));
		}
		if ("true".equals(props.getProperty("test.exec"))) {
		    Exec exec = new Exec(session);
		    exec.test(props.getProperty("exec.command"));
		}
		if ("true".equals(props.getProperty("test.registry"))) {
		    Reg reg = new Reg(session);
		    reg.test(props.getProperty("registry.key"));
		}
		if ("true".equals(props.getProperty("test.wmi"))) {
		    WMI wmi = new WMI(session);
		    wmi.test(props.getProperty("wmi.namespace"), props.getProperty("wmi.query"));
		}
		session.disconnect();
	    } else {
		System.out.println("Failed to connect the session: " + session.getClass().getName());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	System.exit(0);
    }
}

